package org.vvs.valimbavaka.feature.auth.service.impl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.feature.auth.domain.RefreshToken;
import org.vvs.valimbavaka.feature.auth.repository.RefreshTokenRepository;
import org.vvs.valimbavaka.security.RefreshTokenStore;

/** Persistance des refresh tokens ; chaque opération a sa propre transaction (voir AuthService). */
@Service
@Transactional(readOnly = true)
public class RefreshTokenStoreImpl implements RefreshTokenStore {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenStoreImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public void save(UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
        refreshTokenRepository.save(RefreshToken.issue(userId, familyId, tokenHash, expiresAt));
    }

    @Override
    public Optional<StoredRefreshToken> findByHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash).map(token -> new StoredRefreshToken(
                token.getId(), token.getUserId(), token.getFamilyId(), token.getExpiresAt(), token.isRevoked()));
    }

    @Override
    @Transactional
    public void revoke(UUID tokenId) {
        refreshTokenRepository.findById(tokenId).ifPresent(RefreshToken::revoke);
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId) {
        refreshTokenRepository.revokeFamily(familyId);
    }
}
