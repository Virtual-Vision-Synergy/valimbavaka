package org.vvs.valimbavaka.security.audit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Auteur des écritures auditées (created_by / updated_by) : l'utilisateur connecté.
 * Vide hors requête authentifiée (migrations, initialisation) : un auteur inconnu vaut mieux qu'un identifiant inventé.
 */
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(authentication -> authentication.isAuthenticated()
                        && !(authentication instanceof AnonymousAuthenticationToken))
                .map(authentication -> authentication.getPrincipal())
                .filter(AuditableUser.class::isInstance)
                .map(principal -> ((AuditableUser) principal).getId());
    }
}
