package org.vvs.valimbavaka.feature.article.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/** Read DTO for the article sheet. */
public record ArticleRDto(
        UUID id,
        String code,
        String name,
        @Nullable String barcode,
        ArticleType type,
        @Nullable UUID rangeId,
        ArticleUnit unit,
        @Nullable BigDecimal cost,
        @Nullable BigDecimal price,
        BigDecimal threshold,
        @Nullable UUID supplierId,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
