package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.dto.StockAlertRDto;
import org.vvs.valimbavaka.feature.stock.mapper.StockMapper;
import org.vvs.valimbavaka.feature.stock.repository.StockBalance;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** STK-04: threshold alerts computed from the article thresholds and the stock of the active locations. */
@ExtendWith(MockitoExtension.class)
class StockServiceImplAlertTest {

    private static final UUID LOW_ID = UUID.randomUUID();
    private static final UUID OUT_ID = UUID.randomUUID();
    private static final UUID OK_ID = UUID.randomUUID();
    private static final StockLocationRDto WAREHOUSE =
            new StockLocationRDto(UUID.randomUUID(), "WAREHOUSE", "Entrepôt");
    private static final StockLocationRDto STORE = new StockLocationRDto(UUID.randomUUID(), "STORE", "Boutique");

    @Mock
    private ArticleService articleService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private StockMovementRepository stockMovementRepository;

    private StockServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new StockServiceImpl(articleService, referentialService, stockMovementRepository, new StockMapper());
    }

    @Test
    void getAlerts_mixedStocks_returnsOnlyArticlesUnderThresholdMostUrgentFirst() {
        // Arrange
        givenThreeArticlesWithThreshold40();

        // Act
        var result = sut.getAlerts(new PageQuery(1, 20, null, null, null));

        // Assert
        assertThat(result.items()).extracting(StockAlertRDto::articleId).containsExactly(OUT_ID, LOW_ID);
        assertThat(result.totalCount()).isEqualTo(2);
        var low = result.items().get(1);
        assertThat(low.quantity()).isEqualByComparingTo("12.5");
        assertThat(low.shortage()).isEqualByComparingTo("27.5");
        assertThat(low.suggestedQuantity()).isEqualByComparingTo("30");
    }

    @Test
    void getAlerts_secondPageOfSizeOne_returnsTheLessUrgentAlert() {
        // Arrange
        givenThreeArticlesWithThreshold40();

        // Act
        var result = sut.getAlerts(new PageQuery(2, 1, null, null, null));

        // Assert
        assertThat(result.items()).extracting(StockAlertRDto::articleId).containsExactly(LOW_ID);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void getAlerts_pagePastTheEnd_returnsEmptyItemsWithTheTotal() {
        // Arrange
        givenThreeArticlesWithThreshold40();

        // Act
        var result = sut.getAlerts(new PageQuery(5, 20, null, null, null));

        // Assert
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void getAlerts_search_keepsOnlyTheMatchingCodeOrName() {
        // Arrange
        givenThreeArticlesWithThreshold40();

        // Act
        var result = sut.getAlerts(new PageQuery(1, 20, " art-0002 ", null, null));

        // Assert
        assertThat(result.items()).extracting(StockAlertRDto::code).containsExactly("ART-0002");
    }

    @Test
    void getAlerts_noArticleWithThreshold_returnsEmptyPageWithoutReadingBalances() {
        // Arrange
        when(articleService.getArticlesWithThreshold()).thenReturn(List.of());
        when(referentialService.getStockLocations(any())).thenReturn(locations());

        // Act
        var result = sut.getAlerts(new PageQuery(1, 20, null, null, null));

        // Assert
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
        verify(stockMovementRepository, never()).balances(anyCollection());
    }

    /** ART-0001 at 12.5 (low), ART-0002 never moved (out), ART-0003 at exactly 40 (no alert). */
    private void givenThreeArticlesWithThreshold40() {
        when(articleService.getArticlesWithThreshold())
                .thenReturn(List.of(
                        StockTestData.article(LOW_ID, "ART-0001", "40"),
                        StockTestData.article(OUT_ID, "ART-0002", "40"),
                        StockTestData.article(OK_ID, "ART-0003", "40")));
        when(referentialService.getStockLocations(any())).thenReturn(locations());
        when(stockMovementRepository.balances(anyCollection()))
                .thenReturn(List.of(
                        new StockBalance(LOW_ID, WAREHOUSE.id(), new BigDecimal("10")),
                        new StockBalance(LOW_ID, STORE.id(), new BigDecimal("2.5")),
                        new StockBalance(OK_ID, STORE.id(), new BigDecimal("40"))));
    }

    private static PageResponse<StockLocationRDto> locations() {
        return new PageResponse<>(List.of(WAREHOUSE, STORE), 1, 100, 2, 1);
    }
}
