package org.vvs.valimbavaka.feature.inventory.dto;

import jakarta.validation.constraints.NotNull;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryReason;

/** Gap reason of one line (INV-03): only the reason; the line and its gap come from the URL and the server. */
public record InventoryReasonWDto(
        @NotNull(message = "Le motif de l'écart est obligatoire")
        InventoryReason reason) {}
