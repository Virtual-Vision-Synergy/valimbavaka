package org.vvs.valimbavaka.feature.article.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Editable article sheet data, grouped to respect the parameter limit. */
public record ArticleDetails(
        String name,
        @Nullable String barcode,
        ArticleType type,
        @Nullable UUID rangeId,
        ArticleUnit unit,
        @Nullable BigDecimal cost,
        @Nullable BigDecimal price,
        BigDecimal threshold,
        @Nullable UUID supplierId) {}
