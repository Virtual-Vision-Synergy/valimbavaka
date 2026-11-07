package org.vvs.valimbavaka.feature.day.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningLineRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningRDto;
import org.vvs.valimbavaka.feature.day.dto.DayQuantityRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

/**
 * Builds the morning dispatch grid: active finished articles holding stock at the warehouse, by name, and every
 * active location but the warehouse as destination. Raw materials and semi-finished goods stay at the warehouse.
 * Each line carries the quantities of the previous working day (JR-02).
 */
@Component
class DayOpeningGrid {

    /** 20 pages of 100 articles: far beyond the catalogue of the shop, and a bound on the reading. */
    static final int MAX_PAGES = 20;

    private static final int PAGE_SIZE = 100;
    private static final ArticleFilter FINISHED_ACTIVE = new ArticleFilter(ArticleType.FINISHED, null, null, true);
    private static final PageQuery ALL_LOCATIONS = new PageQuery(1, 100, null, "code", null);

    private final ArticleService articleService;
    private final StockService stockService;
    private final ReferentialService referentialService;
    private final PreviousDayQuantities previousDayQuantities;

    DayOpeningGrid(
            ArticleService articleService,
            StockService stockService,
            ReferentialService referentialService,
            PreviousDayQuantities previousDayQuantities) {
        this.articleService = articleService;
        this.stockService = stockService;
        this.referentialService = referentialService;
        this.previousDayQuantities = previousDayQuantities;
    }

    DayOpeningRDto build(LocalDate date, @Nullable BusinessDay previousDay, @Nullable BusinessDayRDto day) {
        var warehouseId = referentialService.requireActiveStockLocationId(DayOpeningPolicy.WAREHOUSE_CODE);
        var destinations = referentialService.getStockLocations(ALL_LOCATIONS).items().stream()
                .filter(location -> !location.id().equals(warehouseId))
                .toList();
        var previousDate = previousDay == null ? null : previousDay.getBusinessDate();
        var lines = lines(warehouseId, previousDayQuantities.of(previousDay));
        return new DayOpeningRDto(date, previousDate, day, warehouseId, destinations, lines);
    }

    private List<DayOpeningLineRDto> lines(UUID warehouseId, Map<UUID, List<DayQuantityRDto>> yesterday) {
        var articles = finishedArticles();
        var stock = stockService.getQuantities(
                warehouseId, articles.stream().map(ArticleRDto::id).toList());
        var lines = new ArrayList<DayOpeningLineRDto>();
        for (var article : articles) {
            var warehouse = stock.getOrDefault(article.id(), BigDecimal.ZERO);
            if (warehouse.signum() > 0) {
                var sent = yesterday.getOrDefault(article.id(), List.of());
                lines.add(new DayOpeningLineRDto(
                        article.id(),
                        article.code(),
                        article.name(),
                        article.unit(),
                        article.threshold(),
                        warehouse,
                        sent));
            }
        }
        return lines;
    }

    private List<ArticleRDto> finishedArticles() {
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
