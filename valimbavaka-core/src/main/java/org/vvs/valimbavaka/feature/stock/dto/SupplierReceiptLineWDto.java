package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One received article, its quantity (3 decimals, like the stock) and its unit purchase cost in ariary. */
public record SupplierReceiptLineWDto(
        @NotNull(message = "L'article est obligatoire") UUID articleId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        @Digits(integer = 9, fraction = 3, message = "La quantité accepte 9 chiffres et 3 décimales au maximum")
        BigDecimal quantity,

        @NotNull(message = "Le coût unitaire est obligatoire")
        @DecimalMin(value = "0", message = "Le coût unitaire ne peut pas être négatif")
        @Digits(integer = 12, fraction = 2, message = "Le coût unitaire a trop de chiffres")
        BigDecimal unitCost) {}
