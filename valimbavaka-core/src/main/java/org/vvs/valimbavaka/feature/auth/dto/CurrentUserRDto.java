package org.vvs.valimbavaka.feature.auth.dto;

import java.util.Set;
import java.util.UUID;

/** Utilisateur connecté : son rôle et ses permissions pilotent le menu côté interface. */
public record CurrentUserRDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        String roleLabel,
        Set<String> permissions,
        boolean passwordChangeRequired) {
}
