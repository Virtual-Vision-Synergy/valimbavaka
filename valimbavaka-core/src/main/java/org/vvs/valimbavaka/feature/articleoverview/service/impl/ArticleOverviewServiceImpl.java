package org.vvs.valimbavaka.feature.articleoverview.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.articleoverview.domain.ArticleBatchRole;
import org.vvs.valimbavaka.feature.articleoverview.dto.ArticleBatchRDto;
import org.vvs.valimbavaka.feature.articleoverview.dto.ArticleOverviewRDto;
import org.vvs.valimbavaka.feature.articleoverview.service.ArticleOverviewService;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionInputRDto;
import org.vvs.valimbavaka.feature.production.service.ProductionBatchService;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

@Service
@Transactional(readOnly = true)
public class ArticleOverviewServiceImpl implements ArticleOverviewService {

    /** Production batches and stock movements shown on the sheet; the full lists stay on their own screens. */
    static final int RECENT_LIMIT = 10;

    private final ArticleService articleService;
    private final StockService stockService;
    private final StockMovementService stockMovementService;
    private final PromotionService promotionService;
    private final ProductionBatchService productionBatchService;

    ArticleOverviewServiceImpl(
            ArticleService articleService,
            StockService stockService,
            StockMovementService stockMovementService,
            PromotionService promotionService,
            ProductionBatchService productionBatchService) {
        this.articleService = articleService;
        this.stockService = stockService;
        this.stockMovementService = stockMovementService;
        this.promotionService = promotionService;
        this.productionBatchService = productionBatchService;
    }

    @Override
    public ArticleOverviewRDto getOverview(UUID articleId) {
        var article = articleService.getArticleById(articleId);
        return new ArticleOverviewRDto(
                article,
                stockService.getStock(articleId),
                activePromotions(article),
                batches(article.id()),
                stockMovementService.getRecentMovements(articleId, RECENT_LIMIT));
    }

    /** Only a finished product is sold, so only it can be under a promotion (ART-01, PRO-01). */
    private List<PromotionRDto> activePromotions(ArticleRDto article) {
        if (article.type() != ArticleType.FINISHED) {
            return List.of();
        }
        return promotionService.getActivePromotionsFor(article.id(), article.rangeId());
    }

    private List<ArticleBatchRDto> batches(UUID articleId) {
        var batches = productionBatchService.getRecentBatchesOf(articleId, RECENT_LIMIT);
        var outputIds =
                batches.stream().map(ProductionBatchRDto::outputArticleId).toList();
        var outputs = articleService.getArticlesByIds(outputIds).stream()
                .collect(Collectors.toMap(ArticleRDto::id, Function.identity()));
        return batches.stream().map(batch -> toRDto(batch, articleId, outputs)).toList();
    }

    /** The output article always exists: {@code production_batches.output_article_id} references the articles. */
    private static ArticleBatchRDto toRDto(ProductionBatchRDto batch, UUID articleId, Map<UUID, ArticleRDto> byId) {
        var output = byId.get(batch.outputArticleId());
        if (output == null) {
            throw new IllegalStateException("Article produit du lot introuvable : " + batch.outputArticleId());
        }
        var produced = output.id().equals(articleId);
        return new ArticleBatchRDto(
                batch.id(),
                batch.number(),
                batch.status(),
                produced ? ArticleBatchRole.OUTPUT : ArticleBatchRole.INPUT,
                produced ? batch.quantity() : consumed(batch, articleId),
                output.id(),
                output.code(),
                output.name(),
                batch.createdAt(),
                batch.completedAt());
    }

    /** An article appears at most once among the inputs of a batch (PRD-01). */
    private static BigDecimal consumed(ProductionBatchRDto batch, UUID articleId) {
        return batch.inputs().stream()
                .filter(input -> input.articleId().equals(articleId))
                .map(ProductionInputRDto::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
