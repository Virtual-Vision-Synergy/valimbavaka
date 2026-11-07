package org.vvs.valimbavaka.feature.production.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchWDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionInputWDto;

final class ProductionTestData {

    static final UUID OUTPUT_ID = UUID.randomUUID();
    static final UUID OPERATOR_ID = UUID.randomUUID();
    static final UUID OIL_ID = UUID.randomUUID();
    static final UUID BASE_ID = UUID.randomUUID();

    private ProductionTestData() {}

    static ProductionBatchWDto request(UUID... inputIds) {
        var inputs = List.of(inputIds).stream()
                .map(id -> new ProductionInputWDto(id, new BigDecimal("0.45")))
                .toList();
        return new ProductionBatchWDto(OUTPUT_ID, new BigDecimal("30"), OPERATOR_ID, inputs);
    }

    static ArticleRDto finishedProduct() {
        return new ArticleRDto(
                OUTPUT_ID,
                "ART-0031",
                "Savon noir 100 g",
                null,
                ArticleType.FINISHED,
                null,
                ArticleUnit.PIECE,
                null,
                null,
                BigDecimal.TEN,
                null,
                true,
                Instant.parse("2026-11-08T08:00:00Z"),
                Instant.parse("2026-11-08T08:00:00Z"),
                0);
    }
}
