package org.vvs.valimbavaka.feature.articleoverview.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.articleoverview.domain.ArticleBatchRole;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionInputRDto;
import org.vvs.valimbavaka.feature.production.service.ProductionBatchService;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

@ExtendWith(MockitoExtension.class)
class ArticleOverviewServiceImplTest {

    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();
    private static final UUID BASE = UUID.randomUUID();
    private static final UUID BOTANIKA = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-08T07:30:00Z");

    @Mock
    private ArticleService articleService;

    @Mock
    private StockService stockService;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private ProductionBatchService productionBatchService;

    private ArticleOverviewServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ArticleOverviewServiceImpl(
                articleService, stockService, stockMovementService, promotionService, productionBatchService);
    }

    @Test
    void getOverview_finishedProduct_assemblesTheFourBlocksWithTheTenMostRecentItems() {
        // Arrange
        var soap = article(SOAP, "ART-0001", ArticleType.FINISHED, BOTANIKA);
        var stock = mock(StockRDto.class);
        var promotion = mock(PromotionRDto.class);
        var movement = movement(SOAP, "30");
        when(articleService.getArticleById(SOAP)).thenReturn(soap);
        when(stockService.getStock(SOAP)).thenReturn(stock);
        when(promotionService.getActivePromotionsFor(SOAP, BOTANIKA)).thenReturn(List.of(promotion));
        when(productionBatchService.getRecentBatchesOf(SOAP, 10))
                .thenReturn(List.of(batch("LOT-00012", "30", input(OIL, "0.45"))));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of(soap));
        when(stockMovementService.getRecentMovements(SOAP, 10)).thenReturn(List.of(movement));

        // Act
        var overview = sut.getOverview(SOAP);

        // Assert
        assertThat(overview.article()).isSameAs(soap);
        assertThat(overview.stock()).isSameAs(stock);
        assertThat(overview.activePromotions()).containsExactly(promotion);
        assertThat(overview.recentMovements()).containsExactly(movement);
        var lot = overview.productionBatches().getFirst();
        assertThat(lot.number()).isEqualTo("LOT-00012");
        assertThat(lot.role()).isEqualTo(ArticleBatchRole.OUTPUT);
        assertThat(lot.quantity()).isEqualByComparingTo("30");
        assertThat(lot.outputArticleCode()).isEqualTo("ART-0001");
        assertThat(lot.status()).isEqualTo(ProductionBatchStatus.COMPLETED);
    }

    @Test
    void getOverview_rawMaterial_skipsPromotionsAndGivesTheConsumedQuantity() {
        // Arrange
        var oil = article(OIL, "ART-0002", ArticleType.RAW_MATERIAL, null);
        var soap = article(SOAP, "ART-0001", ArticleType.FINISHED, BOTANIKA);
        when(articleService.getArticleById(OIL)).thenReturn(oil);
        when(productionBatchService.getRecentBatchesOf(OIL, 10))
                .thenReturn(List.of(batch("LOT-00013", "30", input(BASE, "1.5"), input(OIL, "0.45"))));
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of(soap));

        // Act
        var overview = sut.getOverview(OIL);

        // Assert
        assertThat(overview.activePromotions()).isEmpty();
        var lot = overview.productionBatches().getFirst();
        assertThat(lot.role()).isEqualTo(ArticleBatchRole.INPUT);
        assertThat(lot.quantity()).isEqualByComparingTo("0.45");
        assertThat(lot.outputArticleId()).isEqualTo(SOAP);
        assertThat(lot.outputArticleName()).isEqualTo("Article ART-0001");
        verify(promotionService, never()).getActivePromotionsFor(any(), any());
    }

    @Test
    void getOverview_unknownArticle_throwsNotFoundWithoutReadingTheOtherBlocks() {
        // Arrange
        var unknown = UUID.randomUUID();
        when(articleService.getArticleById(unknown)).thenThrow(new NotFoundException("Article introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.getOverview(unknown)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(stockService, stockMovementService, promotionService, productionBatchService);
    }

    private static ProductionBatchRDto batch(String number, String quantity, ProductionInputRDto... inputs) {
        return new ProductionBatchRDto(
                UUID.randomUUID(),
                number,
                SOAP,
                new BigDecimal(quantity),
                UUID.randomUUID(),
                ProductionBatchStatus.COMPLETED,
                List.of(inputs),
                NOW,
                NOW,
                NOW,
                1L);
    }

    private static StockMovementRDto movement(UUID articleId, String quantity) {
        return new StockMovementRDto(
                UUID.randomUUID(),
                articleId,
                UUID.randomUUID(),
                StockMovementType.PRODUCTION,
                new BigDecimal(quantity),
                null,
                NOW);
    }

    private static ProductionInputRDto input(UUID articleId, String quantity) {
        return new ProductionInputRDto(articleId, new BigDecimal(quantity), null);
    }

    private static ArticleRDto article(UUID id, String code, ArticleType type, @Nullable UUID rangeId) {
        return new ArticleRDto(
                id,
                code,
                "Article " + code,
                null,
                type,
                rangeId,
                ArticleUnit.PIECE,
                null,
                null,
                BigDecimal.ZERO,
                null,
                true,
                NOW,
                NOW,
                0L);
    }
}
