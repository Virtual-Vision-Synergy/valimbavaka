package org.vvs.valimbavaka.security;

/** Résultat d'une connexion ou d'un refresh : le refresh token n'est jamais renvoyé dans le corps JSON. */
public record TokenPair(String accessToken, long expiresInSeconds, String refreshToken, AuthenticatedUser user) {
}
