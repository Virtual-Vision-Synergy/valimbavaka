package org.vvs.valimbavaka.iam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de lecture pour {@link org.vvs.valimbavaka.iam.domain.User}
 */
public record UserRDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        RoleRDto role,
        boolean active,
        boolean mustChangePassword,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
