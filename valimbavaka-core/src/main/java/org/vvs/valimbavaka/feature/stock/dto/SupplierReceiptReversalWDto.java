package org.vvs.valimbavaka.feature.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reason of the reversal of a supplier receipt (STK-06), written in the journal. */
public record SupplierReceiptReversalWDto(
        @NotBlank(message = "Le motif est obligatoire")
        @Size(max = 500, message = "Le motif ne doit pas dépasser {max} caractères")
        String reason) {}
