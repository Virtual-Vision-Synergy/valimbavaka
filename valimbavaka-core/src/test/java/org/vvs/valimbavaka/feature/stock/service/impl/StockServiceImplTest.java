package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.dto.StockLocationQuantityRDto;
import org.vvs.valimbavaka.feature.stock.mapper.StockMapper;
import org.vvs.valimbavaka.feature.stock.repository.StockBalance;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final StockLocationRDto WAREHOUSE =
            new StockLocationRDto(UUID.randomUUID(), "WAREHOUSE", "Entrepôt");
    private static final StockLocationRDto STORE = new StockLocationRDto(UUID.randomUUID(), "STORE", "Boutique");
    private static final PageQuery QUERY = new PageQuery(1, 20, null, null, null);

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
    void getStocks_articleWithMovements_returnsQuantityPerLocationAndTotal() {
        // Arrange
        var page = new PageResponse<>(List.of(StockTestData.article(ARTICLE_ID, "ART-0031")), 1, 20, 1, 1);
        when(articleService.getArticles(eq(QUERY), any(ArticleFilter.class))).thenReturn(page);
        when(referentialService.getStockLocations(any())).thenReturn(locations());
        when(stockMovementRepository.balances(List.of(ARTICLE_ID)))
                .thenReturn(List.of(
                        new StockBalance(ARTICLE_ID, WAREHOUSE.id(), new BigDecimal("30.000")),
                        new StockBalance(ARTICLE_ID, STORE.id(), new BigDecimal("12.500"))));

        // Act
        var result = sut.getStocks(QUERY);

        // Assert
        var stock = result.items().getFirst();
        assertThat(stock.total()).isEqualByComparingTo("42.5");
        assertThat(stock.locations())
                .extracting(StockLocationQuantityRDto::code, StockLocationQuantityRDto::quantity)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(tuple("WAREHOUSE", new BigDecimal("30")), tuple("STORE", new BigDecimal("12.5")));
    }

    @Test
    void getStocks_onlyActiveArticlesAreRequested() {
        // Arrange
        var empty = new PageResponse<ArticleRDto>(List.of(), 1, 20, 0, 0);
        when(articleService.getArticles(eq(QUERY), eq(new ArticleFilter(null, null, null, true))))
                .thenReturn(empty);
        when(referentialService.getStockLocations(any())).thenReturn(locations());

        // Act
        var result = sut.getStocks(QUERY);

        // Assert
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    void getStocks_emptyPage_doesNotQueryTheBalances() {
        // Arrange
        var empty = new PageResponse<ArticleRDto>(List.of(), 1, 20, 0, 0);
        when(articleService.getArticles(eq(QUERY), any(ArticleFilter.class))).thenReturn(empty);
        when(referentialService.getStockLocations(any())).thenReturn(locations());

        // Act
        sut.getStocks(QUERY);

        // Assert
        verify(stockMovementRepository, never()).balances(any());
    }

    @Test
    void getStock_articleWithoutMovement_returnsZeroInEveryLocation() {
        // Arrange
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(StockTestData.article(ARTICLE_ID, "ART-0031"));
        when(referentialService.getStockLocations(any())).thenReturn(locations());
        when(stockMovementRepository.balances(List.of(ARTICLE_ID))).thenReturn(List.of());

        // Act
        var stock = sut.getStock(ARTICLE_ID);

        // Assert
        assertThat(stock.total()).isEqualByComparingTo("0");
        assertThat(stock.locations()).hasSize(2);
    }

    private static PageResponse<StockLocationRDto> locations() {
        return new PageResponse<>(List.of(WAREHOUSE, STORE), 1, 100, 2, 1);
    }
}
