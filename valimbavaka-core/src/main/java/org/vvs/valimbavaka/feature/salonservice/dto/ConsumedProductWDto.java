package org.vvs.valimbavaka.feature.salonservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Product usually consumed by a salon service: active article and quantity in its unit (three decimals). */
public record ConsumedProductWDto(
        @NotNull(message = "L'article consommé est obligatoire")
        UUID articleId,

        @NotNull(message = "La quantité consommée est obligatoire")
        @DecimalMin(value = "0.001", message = "La quantité consommée doit être supérieure à zéro")
        @Digits(integer = 9, fraction = 3, message = "La quantité consommée accepte 3 décimales au maximum")
        BigDecimal quantity) {}
