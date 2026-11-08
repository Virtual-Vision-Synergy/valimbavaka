package org.vvs.valimbavaka.feature.delivery.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One article to deliver and its quantity (same precision as the stock: 3 decimals); the price is read server side. */
public record DeliveryOrderLineWDto(
        @NotNull(message = "L'article est obligatoire") UUID articleId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        @Digits(integer = 9, fraction = 3, message = "La quantité accepte 9 chiffres et 3 décimales au maximum")
        BigDecimal quantity) {}
