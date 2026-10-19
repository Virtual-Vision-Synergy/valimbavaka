package org.vvs.valimbavaka.feature.auth.dto;

/** Réponse de connexion/refresh ; le refresh token voyage uniquement dans un cookie HttpOnly. */
public record AuthRDto(String accessToken, String tokenType, long expiresIn, CurrentUserRDto user) {

    public static final String BEARER = "Bearer";
}
