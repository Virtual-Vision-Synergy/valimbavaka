package org.vvs.valimbavaka.feature.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryArticle;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class InventoryScopeResolverTest {

    private static final UUID ACTIVE_ID = UUID.randomUUID();
    private static final UUID INACTIVE_WITH_STOCK_ID = UUID.randomUUID();
    private static final UUID INACTIVE_EMPTY_ID = UUID.randomUUID();

    @Mock
    private ArticleService articleService;

    @Mock
    private StockService stockService;

    private InventoryScopeResolver sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryScopeResolver(articleService, stockService);
    }

    @Test
    void articlesToCount_inactiveArticles_keepsOnlyThoseStillInStock() {
        // Arrange
        var articles = List.of(
                InventoryTestData.article(ACTIVE_ID, "ART-0031", true),
                InventoryTestData.article(INACTIVE_WITH_STOCK_ID, "ART-0087", false),
                InventoryTestData.article(INACTIVE_EMPTY_ID, "ART-0201", false));
        when(articleService.getArticles(any(PageQuery.class), any(ArticleFilter.class)))
                .thenReturn(new PageResponse<>(articles, 1, 100, 3, 1));
        when(stockService.getQuantities(
                        InventoryTestData.LOCATION_ID, List.of(INACTIVE_WITH_STOCK_ID, INACTIVE_EMPTY_ID)))
                .thenReturn(
                        Map.of(INACTIVE_WITH_STOCK_ID, new BigDecimal("2.000"), INACTIVE_EMPTY_ID, BigDecimal.ZERO));

        // Act
        var result = sut.articlesToCount(InventoryTestData.opening(null));

        // Assert
        assertThat(result).extracting(InventoryArticle::code).containsExactly("ART-0031", "ART-0087");
        assertThat(result.getFirst().barcode()).isEqualTo("3210000000001");
    }

    @Test
    void articlesToCount_twoPages_readsEveryPage() {
        // Arrange
        var first = InventoryTestData.article(UUID.randomUUID(), "ART-0001", true);
        var second = InventoryTestData.article(UUID.randomUUID(), "ART-0101", true);
        when(articleService.getArticles(argThat(query -> query != null && query.page() == 1), any(ArticleFilter.class)))
                .thenReturn(new PageResponse<>(List.of(first), 1, 100, 101, 2));
        when(articleService.getArticles(argThat(query -> query != null && query.page() == 2), any(ArticleFilter.class)))
                .thenReturn(new PageResponse<>(List.of(second), 2, 100, 101, 2));

        // Act
        var result = sut.articlesToCount(InventoryTestData.opening(null));

        // Assert
        assertThat(result).extracting(InventoryArticle::code).containsExactly("ART-0001", "ART-0101");
    }

    @Test
    void articlesToCount_rangeScope_filtersArticlesByRange() {
        // Arrange
        var rangeId = UUID.randomUUID();
        when(articleService.getArticles(any(PageQuery.class), any(ArticleFilter.class)))
                .thenReturn(new PageResponse<>(List.of(), 1, 100, 0, 0));

        // Act
        var result = sut.articlesToCount(InventoryTestData.opening(rangeId));

        // Assert
        assertThat(result).isEmpty();
        verify(articleService).getArticles(any(PageQuery.class), eq(new ArticleFilter(null, rangeId, null, null)));
    }
}
