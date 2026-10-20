package org.vvs.valimbavaka.feature.auth.controller;

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
import org.vvs.valimbavaka.security.jwt.JwtSubject;
import org.vvs.valimbavaka.security.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

/** Login, refresh, logout publics ; {@code /me} exige un access token valide (voir SecurityConfig). */
@Tag(name = "Authentification")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Requête mal formée (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "422", description = "Données invalides (VALIDATION_FAILED)")
})
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
    @SecurityRequirements
    @Operation(summary = "Connexion : le rôle appliqué est toujours celui du compte")
    @ApiResponse(responseCode = "200", description = "Jeton d'accès + cookie de rafraîchissement")
    @ApiResponse(responseCode = "401", description = "Identifiants invalides (UNAUTHENTICATED)")
    @ApiResponse(responseCode = "429", description = "Trop de tentatives, voir Retry-After (RATE_LIMITED)")
    public ResponseEntity<AuthRDto> login(@Valid @RequestBody LoginWDto request) {
        var tokens = authService.login(request.email(), request.password());
        return withRefreshCookie(tokens.refreshToken(), authMapper.toRDto(tokens));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotation du refresh token (cookie HttpOnly)")
    @ApiResponse(responseCode = "200", description = "Nouveau jeton d'accès")
    @ApiResponse(responseCode = "401", description = "Session expirée ou révoquée (UNAUTHENTICATED)")
    @ApiResponse(responseCode = "403", description = "Origine non autorisée (FORBIDDEN)")
    public ResponseEntity<AuthRDto> refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) @Nullable String refreshToken) {
        var tokens = authService.refresh(requireRefreshToken(refreshToken));
        return withRefreshCookie(tokens.refreshToken(), authMapper.toRDto(tokens));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Déconnexion : révoque la session")
    @ApiResponse(responseCode = "204", description = "Session révoquée")
    @ApiResponse(responseCode = "403", description = "Origine non autorisée (FORBIDDEN)")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) @Nullable String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookies.clear()).build();
    }

    @GetMapping("/me")
    @Operation(summary = "Profil et permissions du compte connecté")
    @ApiResponse(responseCode = "200", description = "Profil courant")
    @ApiResponse(responseCode = "401", description = "Non authentifié (UNAUTHENTICATED)")
    public CurrentUserRDto me(@AuthenticationPrincipal Jwt jwt) {
        return authMapper.toRDto(authService.currentUser(JwtSubject.requireUserId(jwt)));
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
