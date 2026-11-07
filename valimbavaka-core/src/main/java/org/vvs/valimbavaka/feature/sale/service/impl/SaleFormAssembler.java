package org.vvs.valimbavaka.feature.sale.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.sale.dto.SaleArticleRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleChannelOptionRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleFormRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleSellerRDto;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

/**
 * Builds the sale form: active channels with their stock location, active sellers by first name (100 at most) and
 * active finished articles with a price, by name, with their stock in the locations of the channels.
 */
@Component
class SaleFormAssembler {

    /** 20 pages of 100 articles: far beyond the catalogue of the shop, and a bound on the reading. */
    static final int MAX_PAGES = 20;

    private static final int PAGE_SIZE = 100;
    private static final ArticleFilter FINISHED_ACTIVE = new ArticleFilter(ArticleType.FINISHED, null, null, true);
    private static final PageQuery SELLERS = new PageQuery(1, 100, null, "firstName", SortDirection.ASC);

    private final SaleChannels channels;
    private final UserService userService;
    private final ArticleService articleService;
    private final StockService stockService;

    SaleFormAssembler(
            SaleChannels channels, UserService userService, ArticleService articleService, StockService stockService) {
        this.channels = channels;
        this.userService = userService;
        this.articleService = articleService;
        this.stockService = stockService;
    }

    SaleFormRDto build(long nextNumber) {
        var options = channels.options();
        var sellers = userService.getActiveSellers(SELLERS).items().stream()
                .map(user -> new SaleSellerRDto(user.id(), user.firstName(), user.lastName(), user.salesChannelId()))
                .toList();
        return new SaleFormRDto(nextNumber, options, sellers, articles(options));
    }

    private List<SaleArticleRDto> articles(List<SaleChannelOptionRDto> options) {
        var articles = sellableArticles();
        var stock = stock(options, articles.stream().map(ArticleRDto::id).toList());
        var result = new ArrayList<SaleArticleRDto>();
        for (var article : articles) {
            var price = article.price();
            if (price != null) {
                result.add(toRDto(article, price, stock.getOrDefault(article.id(), Map.of())));
            }
        }
        return result;
    }

    private static SaleArticleRDto toRDto(ArticleRDto article, BigDecimal price, Map<UUID, BigDecimal> stock) {
        var quantities = Map.copyOf(stock);
        return new SaleArticleRDto(
                article.id(), article.code(), article.barcode(), article.name(), article.rangeId(), price, quantities);
    }

    /** Article id → (location id → quantity), for the distinct locations of the channels. */
    private Map<UUID, Map<UUID, BigDecimal>> stock(List<SaleChannelOptionRDto> options, List<UUID> articleIds) {
        var stock = new HashMap<UUID, Map<UUID, BigDecimal>>();
        var locationIds = options.stream().map(SaleChannelOptionRDto::stockLocationId).distinct().toList();
        for (var locationId : locationIds) {
            for (var entry : stockService.getQuantities(locationId, articleIds).entrySet()) {
                stock.computeIfAbsent(entry.getKey(), id -> new HashMap<>()).put(locationId, entry.getValue());
            }
        }
        return stock;
    }

    private List<ArticleRDto> sellableArticles() {
        var articles = new ArrayList<ArticleRDto>();
        for (var page = 1; page <= MAX_PAGES; page++) {
            var query = new PageQuery(page, PAGE_SIZE, null, "name", SortDirection.ASC);
            var result = articleService.getArticles(query, FINISHED_ACTIVE);
            articles.addAll(result.items());
            if (page >= result.totalPages()) {
                break;
            }
        }
        return articles;
    }
}
