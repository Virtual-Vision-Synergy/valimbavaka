package org.vvs.valimbavaka.feature.transfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One article sent back and its quantity (same precision as the stock: 3 decimals). */
public record TransferLineWDto(
        @NotNull(message = "L'article est obligatoire") UUID articleId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        @Digits(integer = 9, fraction = 3, message = "La quantité accepte 9 chiffres et 3 décimales au maximum")
        BigDecimal quantity) {}
