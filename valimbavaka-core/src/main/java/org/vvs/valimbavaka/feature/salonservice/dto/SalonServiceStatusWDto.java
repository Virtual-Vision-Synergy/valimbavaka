package org.vvs.valimbavaka.feature.salonservice.dto;

import jakarta.validation.constraints.NotNull;

/** Write DTO of a salon service's status: only the active/inactive state can be changed here. */
public record SalonServiceStatusWDto(
        @NotNull(message = "L'état est obligatoire") Boolean active) {}
