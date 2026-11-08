package org.vvs.valimbavaka.feature.commission.dto;

import jakarta.validation.constraints.NotNull;

/** Write DTO of a commission rule's status: only the active/inactive state can be changed here. */
public record CommissionRuleStatusWDto(
        @NotNull(message = "L'état est obligatoire") Boolean active) {}
