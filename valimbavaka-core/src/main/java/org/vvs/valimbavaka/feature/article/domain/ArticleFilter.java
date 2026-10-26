package org.vvs.valimbavaka.feature.article.domain;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Optional filters of the article list; a null criterion means "all". */
public record ArticleFilter(
        @Nullable ArticleType type, @Nullable UUID rangeId, @Nullable UUID supplierId, @Nullable Boolean active) {}
