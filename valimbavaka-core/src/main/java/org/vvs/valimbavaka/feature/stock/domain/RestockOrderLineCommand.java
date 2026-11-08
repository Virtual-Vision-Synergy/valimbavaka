package org.vvs.valimbavaka.feature.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Content of a restock order line, copied from a threshold alert (STK-04) when the order is generated.
 *
 * @param unit name of the article unit ({@code PIECE}, {@code KILOGRAM}, {@code LITER})
 * @param stockQuantity total stock of the active locations at generation time
 * @param quantity quantity to order: the suggested quantity of the alert
 */
public record RestockOrderLineCommand(
        UUID articleId,
        String articleCode,
        String articleName,
        String unit,
        BigDecimal stockQuantity,
        BigDecimal threshold,
        BigDecimal quantity,
        @Nullable UUID supplierId,
        @Nullable String supplierName,
        @Nullable Integer leadTimeDays,
        @Nullable LocalDate expectedDeliveryDate) {}
