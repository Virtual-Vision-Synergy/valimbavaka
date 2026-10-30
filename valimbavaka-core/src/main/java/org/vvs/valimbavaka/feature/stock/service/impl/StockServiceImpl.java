package org.vvs.valimbavaka.feature.stock.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.dto.StockLocationValueRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockValuationRDto;
import org.vvs.valimbavaka.feature.stock.mapper.StockMapper;
import org.vvs.valimbavaka.feature.stock.repository.StockBalance;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@Service
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private static final PageQuery ALL_LOCATIONS = new PageQuery(1, 100, null, "code", null);
    private static final ArticleFilter ACTIVE_ARTICLES = new ArticleFilter(null, null, null, true);
    private static final int MONEY_SCALE = 2;

    private final ArticleService articleService;
    private final ReferentialService referentialService;
    private final StockMovementRepository stockMovementRepository;
    private final StockMapper stockMapper;

    public StockServiceImpl(
            ArticleService articleService,
            ReferentialService referentialService,
            StockMovementRepository stockMovementRepository,
            StockMapper stockMapper) {
        this.articleService = articleService;
        this.referentialService = referentialService;
        this.stockMovementRepository = stockMovementRepository;
        this.stockMapper = stockMapper;
    }

    @Override
    public PageResponse<StockRDto> getStocks(PageQuery query) {
        var articles = articleService.getArticles(query, ACTIVE_ARTICLES);
        var balances =
                balancesByArticle(articles.items().stream().map(ArticleRDto::id).toList());
        var locations = referentialService.getStockLocations(ALL_LOCATIONS).items();
        var items = articles.items().stream()
                .map(article -> toStock(article, locations, balances))
                .toList();
        return new PageResponse<>(
                items, articles.page(), articles.pageSize(), articles.totalCount(), articles.totalPages());
    }

    @Override
    public StockRDto getStock(UUID articleId) {
        var article = articleService.getArticleById(articleId);
        var balances = balancesByArticle(List.of(articleId));
        var locations = referentialService.getStockLocations(ALL_LOCATIONS).items();
        return toStock(article, locations, balances);
    }

    @Override
    public StockValuationRDto getValuation() {
        var balances = stockMovementRepository.nonZeroBalances();
        var articleIds = balances.stream().map(StockBalance::articleId).collect(Collectors.toSet());
        var costs = articleIds.isEmpty() ? Map.<UUID, BigDecimal>of() : articleService.getCosts(articleIds);
        var byLocation = balances.stream().collect(Collectors.groupingBy(StockBalance::locationId));
        var locations = referentialService.getStockLocations(ALL_LOCATIONS).items().stream()
                .map(location -> valueOf(location, byLocation.getOrDefault(location.id(), List.of()), costs))
                .toList();
        return new StockValuationRDto(
                sum(locations, StockLocationValueRDto::quantity),
                sum(locations, StockLocationValueRDto::value),
                sum(locations, StockLocationValueRDto::unvaluedQuantity),
                locations);
    }

    /** Quantity times cost of each article, rounded once per location; an article without cost is not valued. */
    private static StockLocationValueRDto valueOf(
            StockLocationRDto location, List<StockBalance> balances, Map<UUID, BigDecimal> costs) {
        var quantity = BigDecimal.ZERO;
        var value = BigDecimal.ZERO;
        var unvalued = BigDecimal.ZERO;
        for (var balance : balances) {
            quantity = quantity.add(balance.quantity());
            var cost = costs.get(balance.articleId());
            if (cost == null) {
                unvalued = unvalued.add(balance.quantity());
            } else {
                value = value.add(balance.quantity().multiply(cost));
            }
        }
        return new StockLocationValueRDto(
                location.id(),
                location.code(),
                location.label(),
                quantity,
                value.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                unvalued);
    }

    private static BigDecimal sum(
            List<StockLocationValueRDto> locations, Function<StockLocationValueRDto, BigDecimal> field) {
        return locations.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private StockRDto toStock(
            ArticleRDto article, List<StockLocationRDto> locations, Map<UUID, Map<UUID, BigDecimal>> balances) {
        return stockMapper.toRDto(article, locations, balances.getOrDefault(article.id(), Map.of()));
    }

    private Map<UUID, Map<UUID, BigDecimal>> balancesByArticle(List<UUID> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }
        return stockMovementRepository.balances(articleIds).stream()
                .collect(Collectors.groupingBy(
                        StockBalance::articleId,
                        HashMap::new,
                        Collectors.toMap(StockBalance::locationId, StockBalance::quantity)));
    }
}
