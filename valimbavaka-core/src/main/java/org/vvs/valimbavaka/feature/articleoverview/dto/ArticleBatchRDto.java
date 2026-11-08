package org.vvs.valimbavaka.feature.articleoverview.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.articleoverview.domain.ArticleBatchRole;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;

/**
 * A production batch seen from one article (ART-06): {@code quantity} is the quantity of that article (produced if
 * {@code role} is OUTPUT, consumed if INPUT); the output article is given to name what the batch makes.
 */
public record ArticleBatchRDto(
        UUID id,
        String number,
        ProductionBatchStatus status,
        ArticleBatchRole role,
        BigDecimal quantity,
        UUID outputArticleId,
        String outputArticleCode,
        String outputArticleName,
        Instant createdAt,
        @Nullable Instant completedAt) {}
