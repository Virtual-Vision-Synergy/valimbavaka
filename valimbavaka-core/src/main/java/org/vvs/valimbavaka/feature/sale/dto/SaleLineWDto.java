package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Article and quantity sold; the price is never sent by the client, it is read from the article sheet. */
public record SaleLineWDto(
        @NotNull(message = "L'article est obligatoire") UUID articleId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        @Digits(integer = 9, fraction = 3, message = "La quantité accepte 9 chiffres et 3 décimales au maximum")
        BigDecimal quantity) {}
