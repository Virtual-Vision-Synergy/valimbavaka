package org.vvs.valimbavaka.feature.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.vvs.valimbavaka.shared.domain.BaseEntity;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Refresh token opaque : seul son hash SHA-256 est stocké ; la famille permet la révocation en cascade. */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    /** Référence par identifiant : pas d'association JPA entre features (convention §2.3). */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked;

    public static RefreshToken issue(UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
        var token = new RefreshToken();
        token.userId = userId;
        token.familyId = familyId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        return token;
    }

    public void revoke() {
        this.revoked = true;
    }
}
