package org.vvs.valimbavaka.feature.commission.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

/**
 * Recalculation of the computed commissions over the business days {@code [from, to]} (inclusive, both required: the
 * recalculation is always bounded), of one seller or, without {@code sellerId}, of all of them.
 */
public record CommissionRecalculationWDto(
        @NotNull(message = "La date de début est obligatoire") LocalDate from,

        @NotNull(message = "La date de fin est obligatoire") LocalDate to,

        @Nullable UUID sellerId) {}
