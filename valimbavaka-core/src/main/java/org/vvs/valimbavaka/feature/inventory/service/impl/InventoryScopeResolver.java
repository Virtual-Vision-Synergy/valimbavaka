package org.vvs.valimbavaka.feature.inventory.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryArticle;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryOpening;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

/**
 * Articles to count for an opening: every active article of the scope (all, or one range), plus the deactivated
 * ones still holding stock in the location, so that their stock can be adjusted too. Sorted by code.
 */
@Component
class InventoryScopeResolver {

    private static final int PAGE_SIZE = 100;

    private final ArticleService articleService;
    private final StockService stockService;

    InventoryScopeResolver(ArticleService articleService, StockService stockService) {
        this.articleService = articleService;
        this.stockService = stockService;
    }

    List<InventoryArticle> articlesToCount(InventoryOpening opening) {
        var articles = articlesOf(new ArticleFilter(null, opening.rangeId(), null, null));
        var inactiveIds = articles.stream()
                .filter(article -> !article.active())
                .map(ArticleRDto::id)
                .toList();
        var inactiveStock = stockService.getQuantities(opening.locationId(), inactiveIds);
        return articles.stream()
                .filter(article -> article.active() || hasStock(inactiveStock.get(article.id())))
                .map(article -> new InventoryArticle(article.id(), article.code(), article.name(), article.barcode()))
                .toList();
    }

    private List<ArticleRDto> articlesOf(ArticleFilter filter) {
        var articles = new ArrayList<ArticleRDto>();
        var page = 1;
        var totalPages = 1;
        while (page <= totalPages) {
            var result =
                    articleService.getArticles(new PageQuery(page, PAGE_SIZE, null, "code", SortDirection.ASC), filter);
            articles.addAll(result.items());
            totalPages = result.totalPages();
            page++;
        }
        return articles;
    }

    private static boolean hasStock(@Nullable BigDecimal quantity) {
        return quantity != null && quantity.signum() != 0;
    }
}
