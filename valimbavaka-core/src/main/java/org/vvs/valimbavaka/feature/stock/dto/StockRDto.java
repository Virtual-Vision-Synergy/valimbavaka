package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/** Stock of an article: one entry per active location (zero included) and their total. */
public record StockRDto(
        UUID articleId,
        String code,
        String name,
        @Nullable UUID rangeId,
        ArticleUnit unit,
        BigDecimal threshold,
        BigDecimal total,
        List<StockLocationQuantityRDto> locations) {}
