package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/**
 * Line of a restock order, as it was at generation time: {@code stockQuantity} and {@code threshold} of the alert,
 * {@code quantity} to order (suggested quantity of STK-04). Supplier fields are {@code null} for an in-house article.
 */
public record RestockOrderLineRDto(
        int lineNumber,
        UUID articleId,
        String articleCode,
        String articleName,
        ArticleUnit unit,
        BigDecimal stockQuantity,
        BigDecimal threshold,
        BigDecimal quantity,
        @Nullable UUID supplierId,
        @Nullable String supplierName,
        @Nullable Integer leadTimeDays,
        @Nullable LocalDate expectedDeliveryDate) {}
