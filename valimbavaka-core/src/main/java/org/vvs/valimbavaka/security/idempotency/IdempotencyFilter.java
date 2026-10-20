package org.vvs.valimbavaka.security.idempotency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import org.vvs.valimbavaka.core.exception.ProblemDetails;
import org.vvs.valimbavaka.security.handler.ProblemDetailWriter;

/**
 * Rend les écritures rejouables sans doublon (SYS-02) : une requête portant {@code Idempotency-Key} n'est
 * exécutée qu'une fois par utilisateur ; un rejeu (file d'attente hors ligne, coupure en cours d'envoi)
 * reçoit la réponse déjà produite, signalée par {@code Idempotency-Replayed: true}.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String KEY_HEADER = "Idempotency-Key";
    static final String REPLAYED_HEADER = "Idempotency-Replayed";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<Integer> NOT_REPLAYABLE = Set.of(401, 403);

    private final IdempotencyStore store;
    private final ProblemDetailWriter problemWriter;

    public IdempotencyFilter(IdempotencyStore store, ProblemDetailWriter problemWriter) {
        this.store = store;
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader(KEY_HEADER) == null || !WRITE_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var userId = authenticatedUserId();
        if (userId.isEmpty()) {
            chain.doFilter(request, response); // l'autorisation répondra 401
            return;
        }
        var key = parseKey(request.getHeader(KEY_HEADER));
        if (key.isEmpty()) {
            reject(
                    request,
                    response,
                    problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", KEY_HEADER + " doit être un UUID."));
            return;
        }
        var owner = new KeyOwner(userId.get(), key.get());
        if (store.claim(owner.userId(), owner.key(), request.getMethod(), request.getRequestURI())) {
            execute(owner, request, response, chain);
        } else {
            replay(owner, request, response);
        }
    }

    private void execute(KeyOwner owner, HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var wrapper = new ContentCachingResponseWrapper(response);
        var done = false;
        try {
            chain.doFilter(request, wrapper);
            var status = wrapper.getStatus();
            if (status < 500 && !NOT_REPLAYABLE.contains(status)) {
                var body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                store.complete(owner.userId(), owner.key(), status, body);
                done = true;
            }
        } finally {
            if (!done) {
                store.release(owner.userId(), owner.key());
            }
            wrapper.copyBodyToResponse();
        }
    }

    private void replay(KeyOwner owner, HttpServletRequest request, HttpServletResponse response) throws IOException {
        var stored = store.find(owner.userId(), owner.key()).orElse(null);
        if (stored == null || stored.status() == null) { // en cours, ou libérée entre-temps : le client réessaiera
            reject(request, response, problem(HttpStatus.CONFLICT, "CONFLICT", "Requête en cours de traitement."));
        } else if (!stored.method().equals(request.getMethod())
                || !stored.path().equals(request.getRequestURI())) {
            reject(
                    request,
                    response,
                    problem(
                            HttpStatus.UNPROCESSABLE_CONTENT,
                            "VALIDATION_FAILED",
                            KEY_HEADER + " déjà utilisée pour une autre requête."));
        } else {
            writeStored(response, stored.status(), stored.body());
        }
    }

    private void writeStored(HttpServletResponse response, int status, @Nullable String body) throws IOException {
        response.setStatus(status);
        response.setHeader(REPLAYED_HEADER, "true");
        if (body != null && !body.isEmpty()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(body);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, ProblemDetail problem)
            throws IOException {
        problemWriter.write(request, response, problem);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        return ProblemDetails.of(status, code, detail);
    }

    /** Propriétaire d'une clé : une même clé est indépendante d'un utilisateur à l'autre. */
    private record KeyOwner(UUID userId, UUID key) {}

    private static Optional<UUID> authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()) {
            return parseKey(token.getName());
        }
        return Optional.empty();
    }

    private static Optional<UUID> parseKey(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
