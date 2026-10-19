package org.vvs.valimbavaka.security;

import java.util.Optional;
import java.util.UUID;

/** Port défini côté transverse ; la feature des comptes l'implémente (convention §4.2). */
public interface AuthenticatedUserLoader {

    Optional<AuthenticatedUser> findByEmail(String email);

    Optional<AuthenticatedUser> findById(UUID userId);

    void recordFailure(UUID userId, LockPolicy policy);

    void recordSuccess(UUID userId);
}
