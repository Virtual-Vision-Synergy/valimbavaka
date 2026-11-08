package org.vvs.valimbavaka.feature.stock.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.dto.StockAlertRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockValuationRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Read side of the stock (STK-01): quantities per location computed from the movements. */
public interface StockService {

    /** Active articles with their quantity per location and their total; sorting and search follow the articles. */
    PageResponse<StockRDto> getStocks(PageQuery query);

    /** 404 if the article is unknown. */
    StockRDto getStock(UUID articleId);

    /**
     * Active articles whose total stock is under their minimum threshold (STK-04), most urgent first; page, size and
     * search (code or name) are used, the order is fixed.
     */
    PageResponse<StockAlertRDto> getAlerts(PageQuery query);

    /** Every threshold alert, not paged, in the order of {@link #getAlerts}: content of a restock order (STK-05). */
    List<StockAlertRDto> getAllAlerts();

    /**
     * Quantity and value at purchase cost of the whole stock per active location (STK-03), inactive articles
     * included: they are still physically in stock.
     */
    StockValuationRDto getValuation();

    /**
     * Quantity of each given article in one location (inventory, INV-01/INV-02); an article that never moved there
     * is absent from the map (zero). Inactive articles included.
     */
    Map<UUID, BigDecimal> getQuantities(UUID locationId, Collection<UUID> articleIds);
}
