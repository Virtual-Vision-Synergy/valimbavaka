package org.vvs.valimbavaka.feature.auth.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.core.exception.UnauthorizedException;
import org.vvs.valimbavaka.feature.auth.dto.AuthRDto;
import org.vvs.valimbavaka.feature.auth.dto.CurrentUserRDto;
import org.vvs.valimbavaka.feature.auth.dto.LoginWDto;
import org.vvs.valimbavaka.feature.auth.mapper.AuthMapper;
import org.vvs.valimbavaka.security.AuthService;

/** Login, refresh, logout publics ; {@code /me} exige un access token valide (voir SecurityConfig). */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;
    private final RefreshCookieFactory refreshCookies;

    public AuthController(AuthService authService, AuthMapper authMapper, RefreshCookieFactory refreshCookies) {
        this.authService = authService;
        this.authMapper = authMapper;
        this.refreshCookies = refreshCookies;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRDto> login(@Valid @RequestBody LoginWDto request) {
        var tokens = authService.login(request.email(), request.password());
        return withRefreshCookie(tokens.refreshToken(), authMapper.toRDto(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthRDto> refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) @Nullable String refreshToken) {
        var tokens = authService.refresh(requireRefreshToken(refreshToken));
        return withRefreshCookie(tokens.refreshToken(), authMapper.toRDto(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) @Nullable String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookies.clear()).build();
    }

    @GetMapping("/me")
    public CurrentUserRDto me(@AuthenticationPrincipal Jwt jwt) {
        return authMapper.toRDto(authService.currentUser(UUID.fromString(jwt.getSubject())));
    }

    private ResponseEntity<AuthRDto> withRefreshCookie(String refreshToken, AuthRDto body) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookies.issue(refreshToken)).body(body);
    }

    private static String requireRefreshToken(@Nullable String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Session expirée, veuillez vous reconnecter");
        }
        return refreshToken;
    }
}
