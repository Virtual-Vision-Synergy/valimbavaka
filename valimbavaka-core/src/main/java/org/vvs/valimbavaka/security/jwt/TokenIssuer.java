package org.vvs.valimbavaka.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.RefreshTokenStore;
import org.vvs.valimbavaka.security.TokenPair;

/** Émet la paire access token (JWT) + refresh token opaque, ce dernier n'étant persisté que haché. */
@Component
public class TokenIssuer {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokens;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenIssuer(JwtService jwtService, RefreshTokenStore refreshTokens, JwtProperties properties, Clock clock) {
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
        this.clock = clock;
    }

    /** Un refresh token appartient à une famille : celle d'une session, conservée à chaque rotation. */
    public TokenPair issue(AuthenticatedUser user, UUID familyId) {
        var rawRefreshToken = newRefreshToken();
        var expiresAt = clock.instant().plus(properties.refreshTokenTtl());
        refreshTokens.save(user.id(), familyId, hashOf(rawRefreshToken), expiresAt);
        return new TokenPair(
                jwtService.issueAccessToken(user), jwtService.accessTokenTtlSeconds(), rawRefreshToken, user);
    }

    /** Empreinte SHA-256 stockée à la place du jeton : une fuite de la base ne donne aucune session. */
    public String hashOf(String rawRefreshToken) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private String newRefreshToken() {
        var bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
