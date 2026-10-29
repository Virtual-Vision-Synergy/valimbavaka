package org.vvs.valimbavaka.feature.stock.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockLocationQuantityRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;

/** Builds the stock view of an article from its sheet, the active locations and its balances per location. */
@Component
public class StockMapper {

    /** @param balances quantity per location id; a location absent from the map holds zero */
    public StockRDto toRDto(ArticleRDto article, List<StockLocationRDto> locations, Map<UUID, BigDecimal> balances) {
        var quantities = locations.stream()
                .map(location -> new StockLocationQuantityRDto(
                        location.id(),
                        location.code(),
                        location.label(),
                        balances.getOrDefault(location.id(), BigDecimal.ZERO)))
                .toList();
        var total =
                quantities.stream().map(StockLocationQuantityRDto::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StockRDto(
                article.id(),
                article.code(),
                article.name(),
                article.rangeId(),
                article.unit(),
                article.threshold(),
                article.cost(),
                total,
                quantities);
    }
}
