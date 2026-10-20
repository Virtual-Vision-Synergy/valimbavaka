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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
            reject(request, response, HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", KEY_HEADER + " doit être un UUID.");
            return;
        }
        var method = request.getMethod();
        var path = request.getRequestURI();
        if (store.claim(userId.get(), key.get(), method, path)) {
            execute(userId.get(), key.get(), request, response, chain);
        } else {
            replay(userId.get(), key.get(), method + " " + path, request, response);
        }
    }

    private void execute(
            UUID userId, UUID key, HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var wrapper = new ContentCachingResponseWrapper(response);
        var done = false;
        try {
            chain.doFilter(request, wrapper);
            var status = wrapper.getStatus();
            if (status < 500 && !NOT_REPLAYABLE.contains(status)) {
                store.complete(
                        userId, key, status, new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
                done = true;
            }
        } finally {
            if (!done) {
                store.release(userId, key);
            }
            wrapper.copyBodyToResponse();
        }
    }

    private void replay(
            UUID userId, UUID key, String signature, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        var stored = store.find(userId, key);
        if (stored.isEmpty()) { // libérée entre-temps : le client réessaiera
            reject(request, response, HttpStatus.CONFLICT, "CONFLICT", "Requête en cours de traitement.");
        } else if (!(stored.get().method() + " " + stored.get().path()).equals(signature)) {
            reject(
                    request,
                    response,
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "VALIDATION_FAILED",
                    KEY_HEADER + " déjà utilisée pour une autre requête.");
        } else if (stored.get().status() == null) {
            reject(request, response, HttpStatus.CONFLICT, "CONFLICT", "Requête en cours de traitement.");
        } else {
            writeStored(response, stored.get());
        }
    }

    private void writeStored(HttpServletResponse response, IdempotencyStore.StoredRequest stored) throws IOException {
        response.setStatus(stored.status());
        response.setHeader(REPLAYED_HEADER, "true");
        var body = stored.body();
        if (body != null && !body.isEmpty()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(body);
        }
    }

    private void reject(
            HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        problemWriter.write(request, response, ProblemDetails.of(status, code, detail));
    }

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
