package org.vvs.valimbavaka.feature.user.dto;

import java.time.Instant;
import java.util.UUID;

/** DTO de lecture d'un utilisateur ; aucun champ sensible (hash du mot de passe exclu, §7.2). */
public record UserRDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        RoleRDto role,
        boolean active,
        boolean passwordChangeRequired,
        Instant createdAt,
        Instant updatedAt) {
}
