package org.vvs.valimbavaka.feature.stock.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.dto.RestockOrderRDto;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Restock orders (STK-05) generated from the threshold alerts (STK-04), taken back by the receipt (STK-06). */
public interface RestockOrderService {

    /**
     * Creates a {@code PENDING} order with one line per article under its threshold (restricted to
     * {@code request.articleIds()} when given), most urgent first, and traces it in the journal. 409 if no article
     * is left to order.
     */
    RestockOrderRDto generate(RestockOrderWDto request);

    /** Orders with their lines, newest first by default; sort on {@code createdAt} or {@code reference}. */
    PageResponse<RestockOrderRDto> getRestockOrders(PageQuery query);

    /** 404 if the order is unknown. */
    RestockOrderRDto getRestockOrderById(UUID restockOrderId);
}
