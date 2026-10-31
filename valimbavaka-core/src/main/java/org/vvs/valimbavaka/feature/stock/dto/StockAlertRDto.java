package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/**
 * Article under its minimum threshold (STK-04): {@code quantity} is the total over the active locations,
 * {@code shortage} = threshold - quantity, {@code suggestedQuantity} = shortage rounded up to the next ten.
 */
public record StockAlertRDto(
        UUID articleId,
        String code,
        String name,
        ArticleUnit unit,
        BigDecimal quantity,
        BigDecimal threshold,
        BigDecimal shortage,
        BigDecimal suggestedQuantity) {}
