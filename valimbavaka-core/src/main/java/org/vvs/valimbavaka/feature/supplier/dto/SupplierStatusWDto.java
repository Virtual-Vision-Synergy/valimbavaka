package org.vvs.valimbavaka.feature.supplier.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Write DTO for a supplier's status: only the active/inactive state can be changed here.
 */
public record SupplierStatusWDto(
        @NotNull(message = "Status is required") Boolean active) {
}
