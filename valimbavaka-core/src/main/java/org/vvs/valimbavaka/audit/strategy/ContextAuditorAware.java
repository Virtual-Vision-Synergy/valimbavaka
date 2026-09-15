package org.vvs.valimbavaka.audit.strategy;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@SuppressWarnings("NullableProblems")
@Component
public class ContextAuditorAware implements AuditorAware<Long> {
    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Implement logic to retrieve the current user's ID from the authentication object
        return Optional.of(0L);
    }
}