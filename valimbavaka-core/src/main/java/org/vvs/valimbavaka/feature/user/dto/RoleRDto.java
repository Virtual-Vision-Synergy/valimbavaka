package org.vvs.valimbavaka.feature.user.dto;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.vvs.valimbavaka.feature.user.domain.RoleName;

/** DTO de lecture d'un rôle : {@code name} = code (ADMINISTRATEUR…), {@code description} = libellé affiché. */
public record RoleRDto(UUID id, RoleName name, @Nullable String description) {
}
