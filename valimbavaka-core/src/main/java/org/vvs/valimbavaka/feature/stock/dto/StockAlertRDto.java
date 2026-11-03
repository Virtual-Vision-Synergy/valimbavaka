package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/**
 * Article under its minimum threshold (STK-04): {@code quantity} is the total over the active locations,
 * {@code shortage} = threshold - quantity, {@code suggestedQuantity} = shortage rounded up to the next ten.
 * Supplier, lead time and {@code expectedDeliveryDate} (order today + lead time) come from the article's supplier
 * (FRS-04); all {@code null} for an article without supplier (in-house production).
 */
public record StockAlertRDto(
        UUID articleId,
        String code,
        String name,
        ArticleUnit unit,
        BigDecimal quantity,
        BigDecimal threshold,
        BigDecimal shortage,
        BigDecimal suggestedQuantity,
        @Nullable UUID supplierId,
        @Nullable String supplierName,
        @Nullable Integer leadTimeDays,
        @Nullable LocalDate expectedDeliveryDate) {}
