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

    /**
     * Receipt of the order by a supplier receipt (STK-06): {@code PENDING} to {@code RECEIVED}, traced in the journal
     * ({@code UPDATE}). Must be called in the receipt transaction. 404 if unknown, 409 if already received.
     *
     * @param receiptNumber number of the receipt taking the order back, written in the journal
     * @return the order reference ({@code BR-000007})
     */
    String markReceived(UUID restockOrderId, String receiptNumber);
}
