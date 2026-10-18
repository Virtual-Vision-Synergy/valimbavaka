package org.vvs.valimbavaka.feature.user.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** DTO de lecture d'un utilisateur ; aucun champ sensible (hash du mot de passe exclu, §7.2). */
public record UserRDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        RoleRDto role,
        @Nullable UUID salesChannelId,
        @Nullable BigDecimal commissionRate,
        boolean active,
        boolean passwordChangeRequired,
        Instant createdAt,
        Instant updatedAt) {
}
