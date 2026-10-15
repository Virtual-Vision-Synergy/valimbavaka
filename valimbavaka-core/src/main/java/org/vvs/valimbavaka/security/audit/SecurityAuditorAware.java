package org.vvs.valimbavaka.security.audit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * Auteur des écritures auditées (created_by / updated_by).
 * Vide tant que le JWT n'existe pas : un auteur inconnu vaut mieux qu'un identifiant inventé.
 */
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return Optional.empty();
    }
}
