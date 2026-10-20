package org.vvs.valimbavaka.feature.auth.controller;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.vvs.valimbavaka.security.AuthProperties;
import org.vvs.valimbavaka.security.jwt.JwtProperties;

/** Cookie du refresh token : HttpOnly, SameSite=Strict, limité aux routes d'authentification (§9.2). */
@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "vvs_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthProperties authProperties;
    private final JwtProperties jwtProperties;

    public RefreshCookieFactory(AuthProperties authProperties, JwtProperties jwtProperties) {
        this.authProperties = authProperties;
        this.jwtProperties = jwtProperties;
    }

    public String issue(String refreshToken) {
        return build(refreshToken, jwtProperties.refreshTokenTtl());
    }

    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(authProperties.refreshCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
