package org.vvs.valimbavaka.feature.stock.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.dto.StockRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Read side of the stock (STK-01): quantities per location computed from the movements. */
public interface StockService {

    /** Active articles with their quantity per location and their total; sorting and search follow the articles. */
    PageResponse<StockRDto> getStocks(PageQuery query);

    /** 404 if the article is unknown. */
    StockRDto getStock(UUID articleId);
}
