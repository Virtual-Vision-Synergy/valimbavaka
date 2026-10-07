package org.vvs.valimbavaka.iam.dto;

import java.util.UUID;

/**
 * DTO de lecture pour {@link org.vvs.valimbavaka.iam.domain.Role}
 */
public record RoleRDto(UUID id, String name, String description) {
}
