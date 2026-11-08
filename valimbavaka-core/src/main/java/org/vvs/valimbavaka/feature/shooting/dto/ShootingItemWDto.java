package org.vvs.valimbavaka.feature.shooting.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** One article to take out for a shooting, with the quantity taken (positive). */
public record ShootingItemWDto(
        @NotNull(message = "L'article est obligatoire") UUID articleId,

        @NotNull(message = "La quantité est obligatoire")
        @DecimalMin(value = "0", inclusive = false, message = "La quantité doit être positive")
        @Digits(integer = 9, fraction = 3, message = "La quantité accepte 3 décimales au maximum")
        BigDecimal quantity) {}
