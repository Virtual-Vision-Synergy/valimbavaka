package org.vvs.valimbavaka.security.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
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
import org.vvs.valimbavaka.security.jwt.JwtProperties;
import org.vvs.valimbavaka.security.jwt.JwtService;
import org.vvs.valimbavaka.security.service.AuthService;

/**
 * Connexion, rotation du refresh token et déconnexion. Volontairement sans {@code @Transactional} :
 * un échec doit rester enregistré alors qu'une exception est levée (chaque appel au port a sa transaction).
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String INVALID_CREDENTIALS = "Identifiants invalides";
    private static final String INVALID_SESSION = "Session expirée, veuillez vous reconnecter";
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final AuthenticatedUserLoader users;
    private final RefreshTokenStore refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    /** Hash factice calculé une fois avec le même encodeur : temps de réponse comparable si l'email est inconnu. */
    private final String dummyHash;

    public AuthServiceImpl(AuthenticatedUserLoader users, RefreshTokenStore refreshTokens,
            PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties,
            AuthProperties authProperties, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
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
            user.ifPresent(found -> users.recordFailure(found.id(), authProperties.lockPolicy()));
            log.warn("Échec de connexion");
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        users.recordSuccess(user.get().id());
        return issue(user.get(), UUID.randomUUID());
    }

    @Override
    public TokenPair refresh(String rawRefreshToken) {
        var stored = refreshTokens.findByHash(hash(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
        if (stored.revoked()) {
            log.warn("Réutilisation d'un refresh token révoqué : famille {} révoquée", stored.familyId());
            throw revokeFamilyAndReject(stored);
        }
        var user = users.findById(stored.userId()).filter(AuthenticatedUser::enabled)
                .orElseThrow(() -> revokeFamilyAndReject(stored));
        if (!stored.expiresAt().isAfter(clock.instant())) {
            throw new UnauthorizedException(INVALID_SESSION);
        }
        refreshTokens.revoke(stored.id());
        return issue(user, stored.familyId());
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokens.findByHash(hash(rawRefreshToken))
                .ifPresent(stored -> refreshTokens.revokeFamily(stored.familyId()));
    }

    @Override
    public AuthenticatedUser currentUser(UUID userId) {
        return users.findById(userId).filter(AuthenticatedUser::enabled)
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
    }

    private UnauthorizedException revokeFamilyAndReject(StoredRefreshToken stored) {
        refreshTokens.revokeFamily(stored.familyId());
        return new UnauthorizedException(INVALID_SESSION);
    }

    private TokenPair issue(AuthenticatedUser user, UUID familyId) {
        var rawRefresh = newRefreshToken();
        var expiresAt = clock.instant().plus(jwtProperties.refreshTokenTtl());
        refreshTokens.save(user.id(), familyId, hash(rawRefresh), expiresAt);
        return new TokenPair(jwtService.issueAccessToken(user), jwtService.accessTokenTtlSeconds(), rawRefresh, user);
    }

    private boolean isLocked(AuthenticatedUser user) {
        return user.lockedUntil() != null && user.lockedUntil().isAfter(clock.instant());
    }

    private String newRefreshToken() {
        var bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
