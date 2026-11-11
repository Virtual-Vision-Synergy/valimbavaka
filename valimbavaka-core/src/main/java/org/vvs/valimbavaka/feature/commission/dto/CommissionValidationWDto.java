package org.vvs.valimbavaka.feature.commission.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Validation of the computed commissions of one seller over the business days {@code [from, to]} (inclusive). */
public record CommissionValidationWDto(
        @NotNull(message = "La vendeuse est obligatoire") UUID sellerId,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate from,

        @NotNull(message = "La date de fin est obligatoire") LocalDate to) {}
