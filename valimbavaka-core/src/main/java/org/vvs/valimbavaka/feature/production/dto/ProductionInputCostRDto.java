package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/**
 * One input of the cost price of a batch (PRD-05). {@code unitCost}: cost frozen at completion (PRD-02), or current
 * purchase cost while the batch is not completed; null if the article has no cost. {@code lineCost} = quantity x
 * unit cost, rounded to 2 decimals, null without unit cost.
 */
public record ProductionInputCostRDto(
        UUID articleId,
        String articleCode,
        String articleName,
        ArticleUnit unit,
        BigDecimal quantity,
        @Nullable BigDecimal unitCost,
        @Nullable BigDecimal lineCost) {}
