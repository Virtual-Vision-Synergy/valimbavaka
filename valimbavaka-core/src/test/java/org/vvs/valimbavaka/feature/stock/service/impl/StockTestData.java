package org.vvs.valimbavaka.feature.stock.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;

final class StockTestData {

    private StockTestData() {}

    static ArticleRDto article(UUID id, String code) {
        return new ArticleRDto(
                id,
                code,
                "Savon noir 100 g",
                null,
                ArticleType.FINISHED,
                null,
                ArticleUnit.PIECE,
                null,
                null,
                new BigDecimal("40"),
                null,
                true,
                Instant.parse("2026-10-29T08:00:00Z"),
                Instant.parse("2026-10-29T08:00:00Z"),
                0);
    }
}
