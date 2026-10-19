package org.vvs.valimbavaka.security.service.impl;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.vvs.valimbavaka.core.exception.UnauthorizedException;
import org.vvs.valimbavaka.security.AuthProperties;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;
import org.vvs.valimbavaka.security.RefreshTokenStore;
import org.vvs.valimbavaka.security.RefreshTokenStore.StoredRefreshToken;
import org.vvs.valimbavaka.security.TokenPair;
import org.vvs.valimbavaka.security.jwt.TokenIssuer;
import org.vvs.valimbavaka.security.service.AuthService;

/**
 * Volontairement sans {@code @Transactional} : un échec doit rester enregistré alors qu'une exception
 * est levée (chaque appel aux ports a sa propre transaction, convention §4.2).
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String INVALID_CREDENTIALS = "Identifiants invalides";
    private static final String INVALID_SESSION = "Session expirée, veuillez vous reconnecter";

    private final AuthenticatedUserLoader users;
    private final RefreshTokenStore refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokens;
    private final AuthProperties authProperties;
    private final Clock clock;
    /** Hash factice calculé une fois avec le même encodeur : temps de réponse comparable si l'email est inconnu. */
    private final String dummyHash;

    public AuthServiceImpl(AuthenticatedUserLoader users, RefreshTokenStore refreshTokens,
            PasswordEncoder passwordEncoder, TokenIssuer tokens, AuthProperties authProperties, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.authProperties = authProperties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Override
    public TokenPair login(String email, String rawPassword) {
        var user = users.findByEmail(email.strip().toLowerCase(Locale.ROOT));
        var hash = user.map(AuthenticatedUser::passwordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(rawPassword, hash);
        if (user.isEmpty() || !matches || !user.get().enabled() || isLocked(user.get())) {
            user.ifPresentOrElse(this::recordFailure, () -> log.warn("Échec de connexion : compte inconnu"));
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        users.recordSuccess(user.get().id());
        log.info("Utilisateur {} connecté", user.get().id());
        return tokens.issue(user.get(), UUID.randomUUID());
    }

    @Override
    public TokenPair refresh(String rawRefreshToken) {
        var stored = refreshTokens.findByHash(tokens.hashOf(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
        if (stored.revoked()) {
            log.warn("Réutilisation d'un refresh token révoqué : session {} révoquée", stored.familyId());
            throw revokeFamilyAndReject(stored);
        }
        var user = users.findById(stored.userId()).filter(AuthenticatedUser::enabled)
                .orElseThrow(() -> revokeFamilyAndReject(stored));
        if (!stored.expiresAt().isAfter(clock.instant())) {
            throw new UnauthorizedException(INVALID_SESSION);
        }
        refreshTokens.revoke(stored.id());
        return tokens.issue(user, stored.familyId());
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokens.findByHash(tokens.hashOf(rawRefreshToken))
                .ifPresent(stored -> refreshTokens.revokeFamily(stored.familyId()));
    }

    @Override
    public AuthenticatedUser currentUser(UUID userId) {
        return users.findById(userId).filter(AuthenticatedUser::enabled)
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
    }

    private void recordFailure(AuthenticatedUser user) {
        users.recordFailure(user.id(), authProperties.lockPolicy());
        log.warn("Échec de connexion pour l'utilisateur {}", user.id());
    }

    private UnauthorizedException revokeFamilyAndReject(StoredRefreshToken stored) {
        refreshTokens.revokeFamily(stored.familyId());
        return new UnauthorizedException(INVALID_SESSION);
    }

    private boolean isLocked(AuthenticatedUser user) {
        return user.lockedUntil() != null && user.lockedUntil().isAfter(clock.instant());
    }
}
