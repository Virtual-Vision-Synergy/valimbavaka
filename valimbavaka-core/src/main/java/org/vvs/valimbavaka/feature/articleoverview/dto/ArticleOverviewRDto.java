package org.vvs.valimbavaka.feature.articleoverview.dto;

import java.util.List;

import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;

/**
 * Article sheet at 360° (ART-06), read in one call: identity, stock per location, promotions active today that reach
 * the article, its most recent production batches and its most recent stock movements (the journal of the article).
 */
public record ArticleOverviewRDto(
        ArticleRDto article,
        StockRDto stock,
        List<PromotionRDto> activePromotions,
        List<ArticleBatchRDto> productionBatches,
        List<StockMovementRDto> recentMovements) {}
