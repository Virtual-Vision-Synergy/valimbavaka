package org.vvs.valimbavaka.security.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import org.vvs.valimbavaka.support.IntegrationTest;

/** SYS-02, SYS-12 : réservation atomique, mémorisation, libération et purge à 24 h des clés d'idempotence. */
@IntegrationTest
class IdempotencyStoreIT {

    private static final RequestFingerprint REQUEST = new RequestFingerprint("POST", "/api/v1/suppliers", "abc123");

    @Autowired
    private IdempotencyStore sut;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void claim_sameKeyTwice_secondClaimIsRefused() {
        // Arrange
        var user = UUID.randomUUID();
        var key = UUID.randomUUID();

        // Act
        var first = sut.claim(user, key, REQUEST);
        var second = sut.claim(user, key, REQUEST);

        // Assert
        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void claim_sameKeyForTwoUsers_bothAreAccepted() {
        // Arrange
        var key = UUID.randomUUID();

        // Act
        var forFirst = sut.claim(UUID.randomUUID(), key, REQUEST);
        var forSecond = sut.claim(UUID.randomUUID(), key, REQUEST);

        // Assert
        assertThat(forFirst).isTrue();
        assertThat(forSecond).isTrue();
    }

    @Test
    void find_reservedKey_isInProgressWithItsFingerprint() {
        // Arrange
        var user = UUID.randomUUID();
        var key = UUID.randomUUID();
        sut.claim(user, key, REQUEST);

        // Act
        var stored = sut.find(user, key).orElseThrow();

        // Assert
        assertThat(stored.status()).isNull();
        assertThat(stored.request()).isEqualTo(REQUEST);
    }

    @Test
    void complete_thenFind_returnsTheStoredResponse() {
        // Arrange
        var user = UUID.randomUUID();
        var key = UUID.randomUUID();
        sut.claim(user, key, REQUEST);

        // Act
        sut.complete(user, key, 201, "{\"id\":\"1\"}");

        // Assert
        var stored = sut.find(user, key).orElseThrow();
        assertThat(stored.status()).isEqualTo(201);
        assertThat(stored.body()).isEqualTo("{\"id\":\"1\"}");
    }

    @Test
    void release_afterClaim_letsTheSameKeyBeClaimedAgain() {
        // Arrange
        var user = UUID.randomUUID();
        var key = UUID.randomUUID();
        sut.claim(user, key, REQUEST);

        // Act
        sut.release(user, key);

        // Assert
        assertThat(sut.find(user, key)).isEmpty();
        assertThat(sut.claim(user, key, REQUEST)).isTrue();
    }

    @Test
    void claim_keysOlderThan24Hours_arePurgedAndYoungerOnesKept() {
        // Arrange
        var user = UUID.randomUUID();
        var old = UUID.randomUUID();
        var recent = UUID.randomUUID();
        insertKeyCreated(user, old, OffsetDateTime.now().minusHours(25));
        insertKeyCreated(user, recent, OffsetDateTime.now().minusHours(23));

        // Act
        sut.claim(user, UUID.randomUUID(), REQUEST);

        // Assert
        assertThat(sut.find(user, old)).isEmpty();
        assertThat(sut.find(user, recent)).isPresent();
    }

    @Test
    void claim_oldKeyOfTheSameUser_canBeReusedAfterThePurge() {
        // Arrange
        var user = UUID.randomUUID();
        var key = UUID.randomUUID();
        insertKeyCreated(user, key, OffsetDateTime.now().minusHours(25));

        // Act
        var claimed = sut.claim(user, key, REQUEST);

        // Assert
        assertThat(claimed).isTrue();
    }

    private void insertKeyCreated(UUID user, UUID key, OffsetDateTime createdAt) {
        jdbc.sql("""
                        INSERT INTO idempotency_keys
                            (user_id, idempotency_key, http_method, request_path, created_at)
                        VALUES (:user, :key, 'POST', '/api/v1/suppliers', :createdAt)
                        """)
                .param("user", user)
                .param("key", key)
                .param("createdAt", createdAt)
                .update();
    }
}
