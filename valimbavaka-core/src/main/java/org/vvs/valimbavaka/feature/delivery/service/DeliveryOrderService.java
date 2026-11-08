package org.vvs.valimbavaka.feature.delivery.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderFilter;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryDriverRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Orders to deliver (LIV-01). A delivery driver only reads the orders assigned to him: for him the list is limited
 * to his orders and another driver's order is reported as not found.
 */
public interface DeliveryOrderService {

    /**
     * Creates an order {@code TO_PREPARE}, appended at the end of the round of its driver for the day, and traces it
     * in the journal. 422 if the driver is unknown, inactive or not a driver, if the day is in the past, if an article
     * appears twice, is not a finished product or is inactive; 404 if an article is unknown.
     */
    DeliveryOrderRDto createOrder(DeliveryOrderWDto request);

    /** 404 if the order is unknown (or belongs to another driver for a driver). */
    DeliveryOrderRDto getOrder(UUID orderId);

    /**
     * Orders matching the filter, newest first by default; sortable by {@code createdAt}, {@code number},
     * {@code scheduledDate}, {@code stopNumber} and {@code status}.
     */
    PageResponse<DeliveryOrderRDto> getOrders(PageQuery query, DeliveryOrderFilter filter);

    /** Active drivers that can be assigned, by first name; search on first and last name. */
    PageResponse<DeliveryDriverRDto> getDrivers(PageQuery query);
}
