package org.vvs.valimbavaka.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Compte tel que vu par la sécurité. Le rôle et les permissions viennent exclusivement de la base
 * (AUTH-03) : aucune donnée de la requête ne peut les modifier.
 */
public record AuthenticatedUser(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        String roleLabel,
        String passwordHash,
        Set<String> permissions,
        boolean enabled,
        boolean passwordChangeRequired,
        @Nullable Instant lockedUntil) {
}
