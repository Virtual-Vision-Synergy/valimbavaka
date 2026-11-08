package org.vvs.valimbavaka.feature.day.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Quantity of an article sent from the warehouse to one destination (point of sale) in the morning. */
public record DispatchLineWDto(
        @NotNull(message = "L'article est obligatoire") UUID articleId,

        @NotNull(message = "La destination est obligatoire") UUID destinationId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        @Digits(integer = 9, fraction = 3, message = "La quantité accepte 9 chiffres et 3 décimales au maximum")
        BigDecimal quantity) {}
