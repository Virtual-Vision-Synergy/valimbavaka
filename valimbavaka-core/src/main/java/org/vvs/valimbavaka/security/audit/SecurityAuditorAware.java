package org.vvs.valimbavaka.security.audit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.vvs.valimbavaka.security.jwt.JwtSubject;

/** Auteur des écritures auditées : l'identifiant du compte authentifié (claim {@code sub}), sinon vide. */
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return JwtSubject.userIdOf(jwtAuthentication.getToken());
        }
        return Optional.empty();
    }
}
