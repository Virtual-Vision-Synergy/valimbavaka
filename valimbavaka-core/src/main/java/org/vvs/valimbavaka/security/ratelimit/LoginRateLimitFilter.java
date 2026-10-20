package org.vvs.valimbavaka.security.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.vvs.valimbavaka.core.exception.ProblemDetails;
import org.vvs.valimbavaka.security.handler.ProblemDetailWriter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limite {@code POST /api/v1/auth/login} par IP puis par identifiant (email) : 429 + Retry-After.
 * Les seaux vivent dans des caches bornés, un attaquant ne peut donc pas saturer la mémoire.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_BODY_BYTES = 4096;
    private static final long MAX_TRACKED_KEYS = 100_000;

    private final RateLimitProperties properties;
    private final ProblemDetailWriter problemWriter;
    private final JsonMapper jsonMapper;
    private final Cache<String, Bucket> ipBuckets = newCache();
    private final Cache<String, Bucket> identifierBuckets = newCache();

    public LoginRateLimitFilter(RateLimitProperties properties, ProblemDetailWriter problemWriter,
            JsonMapper jsonMapper) {
        this.properties = properties;
        this.problemWriter = problemWriter;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !LOGIN_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            problemWriter.write(request, response, ProblemDetails.of(HttpStatus.BAD_REQUEST,
                    "MALFORMED_REQUEST", "Corps de requête trop volumineux"));
            return;
        }
        var cached = new CachedBodyRequest(request, body);
        var blocking = Optional.ofNullable(consume(ipBuckets, request.getRemoteAddr(), properties.loginPerIp()))
                .or(() -> identifierOf(body)
                        .map(id -> consume(identifierBuckets, id, properties.loginPerIdentifier())));
        if (blocking.isPresent()) {
            reject(cached, response, blocking.get());
            return;
        }
        chain.doFilter(cached, response);
    }

    /** @return la sonde de refus si le seau est vide, {@code null} si la requête passe. */
    private @Nullable ConsumptionProbe consume(Cache<String, Bucket> buckets, String key, int limit) {
        var bucket = buckets.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(limit).refillIntervally(limit, properties.window()).build())
                .build());
        var probe = bucket.tryConsumeAndReturnRemaining(1);
        return probe.isConsumed() ? null : probe;
    }

    private Optional<String> identifierOf(byte[] body) {
        if (body.length == 0 || body.length > MAX_BODY_BYTES) {
            return Optional.empty();
        }
        try {
            var email = jsonMapper.readTree(body).path("email").asString("");
            return email.isBlank() ? Optional.empty() : Optional.of(email.strip().toLowerCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, ConsumptionProbe probe)
            throws IOException {
        var retryAfter = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        problemWriter.write(request, response, ProblemDetails.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Trop de tentatives, réessayez plus tard"));
    }

    private static Cache<String, Bucket> newCache() {
        return Caffeine.newBuilder().maximumSize(MAX_TRACKED_KEYS).expireAfterAccess(Duration.ofHours(1)).build();
    }
}
