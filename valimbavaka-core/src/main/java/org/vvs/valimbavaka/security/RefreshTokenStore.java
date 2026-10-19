package org.vvs.valimbavaka.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Port de persistance des refresh tokens (stockés hachés, jamais en clair). */
public interface RefreshTokenStore {

    void save(UUID userId, UUID familyId, String tokenHash, Instant expiresAt);

    Optional<StoredRefreshToken> findByHash(String tokenHash);

    void revoke(UUID tokenId);

    /** Réutilisation d'un ancien jeton : toute la famille est révoquée (convention §9.2). */
    void revokeFamily(UUID familyId);

    record StoredRefreshToken(UUID id, UUID userId, UUID familyId, Instant expiresAt, boolean revoked) {
    }
}
