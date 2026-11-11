package org.vvs.valimbavaka.feature.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryFailureReason;

/**
 * Failure reported by the driver at the door: a required reason and an optional comment. The order, its status and
 * the stock return are set by the server: sent in the body, they are ignored.
 */
public record DeliveryFailureWDto(
        @NotNull(message = "Le motif de l'échec est obligatoire")
        DeliveryFailureReason reason,

        @Size(max = 280, message = "Le commentaire ne doit pas dépasser {max} caractères") @Nullable
        String note) {}
