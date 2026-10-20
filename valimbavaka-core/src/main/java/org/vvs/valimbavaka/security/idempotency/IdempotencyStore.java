package org.vvs.valimbavaka.security.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Clés d'idempotence (SYS-02). Une ligne sans {@code response_status} signifie « requête en cours » ;
 * la clé est réservée par un INSERT atomique, ce qui sérialise deux rejeux simultanés.
 */
@Component
public class IdempotencyStore {

    static final Duration RETENTION = Duration.ofHours(24);

    private final JdbcClient jdbc;
    private final Clock clock;

    public IdempotencyStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Réserve la clé ; false si elle existe déjà (requête déjà traitée ou en cours). */
    public boolean claim(UUID userId, UUID key, String method, String path) {
        var now = OffsetDateTime.now(clock);
        jdbc.sql("DELETE FROM idempotency_keys WHERE created_at < :limit")
                .param("limit", now.minus(RETENTION))
                .update();
        return jdbc.sql("""
                        INSERT INTO idempotency_keys (user_id, idempotency_key, http_method, request_path, created_at)
                        VALUES (:user, :key, :method, :path, :now)
                        ON CONFLICT DO NOTHING
                        """)
                        .param("user", userId)
                        .param("key", key)
                        .param("method", method)
                        .param("path", path)
                        .param("now", now)
                        .update()
                == 1;
    }

    public Optional<StoredRequest> find(UUID userId, UUID key) {
        return jdbc.sql("""
                        SELECT http_method, request_path, response_status, response_body
                        FROM idempotency_keys WHERE user_id = :user AND idempotency_key = :key
                        """)
                .param("user", userId)
                .param("key", key)
                .query((rs, row) -> new StoredRequest(
                        rs.getString("http_method"),
                        rs.getString("request_path"),
                        (Integer) rs.getObject("response_status"),
                        rs.getString("response_body")))
                .optional();
    }

    public void complete(UUID userId, UUID key, int status, String body) {
        jdbc.sql("""
                        UPDATE idempotency_keys SET response_status = :status, response_body = :body
                        WHERE user_id = :user AND idempotency_key = :key
                        """)
                .param("status", status)
                .param("body", body)
                .param("user", userId)
                .param("key", key)
                .update();
    }

    /** Libère la clé quand la réponse ne doit pas être rejouée (erreur serveur, session) : le client peut réessayer. */
    public void release(UUID userId, UUID key) {
        jdbc.sql("DELETE FROM idempotency_keys WHERE user_id = :user AND idempotency_key = :key")
                .param("user", userId)
                .param("key", key)
                .update();
    }

    /** Requête mémorisée ; {@code status} est nul tant qu'elle n'est pas terminée. */
    public record StoredRequest(
            String method,
            String path,
            @Nullable Integer status,
            @Nullable String body) {}
}
