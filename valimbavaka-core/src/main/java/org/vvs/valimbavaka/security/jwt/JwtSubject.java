package org.vvs.valimbavaka.security.jwt;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.vvs.valimbavaka.core.exception.UnauthorizedException;

/** Lecture de l'identifiant du compte (claim {@code sub}) d'un JWT : le claim est nullable côté bibliothèque. */
public final class JwtSubject {

    private JwtSubject() {
    }

    public static Optional<UUID> userIdOf(Jwt jwt) {
        return Optional.ofNullable(jwt.getSubject()).map(UUID::fromString);
    }

    /** Un jeton sans {@code sub} n'est pas un jeton émis par nous : échec fermé (401). */
    public static UUID requireUserId(Jwt jwt) {
        return userIdOf(jwt).orElseThrow(() -> new UnauthorizedException("Jeton invalide"));
    }
}
