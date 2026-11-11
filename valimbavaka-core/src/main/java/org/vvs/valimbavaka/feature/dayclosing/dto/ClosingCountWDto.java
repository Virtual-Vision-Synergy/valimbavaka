package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import org.vvs.valimbavaka.feature.dayclosing.domain.ClosingGapReason;

/**
 * Evening count of one article in one location (JR-05). Only what the administrator types: the theoretical quantity
 * and the gap are computed by the server, and the reason is required when the count differs from the theoretical
 * quantity (422 otherwise). A reason sent without a gap is ignored.
 */
public record ClosingCountWDto(
        @NotNull(message = "L'emplacement est obligatoire") UUID locationId,
        @NotNull(message = "L'article est obligatoire") UUID articleId,
        @NotNull(message = "La quantité comptée est obligatoire")
        @DecimalMin(value = "0", message = "La quantité comptée ne peut pas être négative")
        @Digits(integer = 11, fraction = 3, message = "La quantité comptée accepte 3 décimales au maximum")
        BigDecimal counted,
        @Nullable ClosingGapReason reason) {}
