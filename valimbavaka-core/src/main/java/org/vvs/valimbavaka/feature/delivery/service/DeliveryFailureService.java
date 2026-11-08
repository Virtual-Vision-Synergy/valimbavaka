package org.vvs.valimbavaka.feature.delivery.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.delivery.dto.DeliveryFailureWDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;

/** Failure of a delivery reported by the driver (LIV-04): the articles come back to stock, no sale is created. */
public interface DeliveryFailureService {

    /**
     * Moves the order to {@code FAILED} with its reason, records a stock return ({@code RET-…}) of its articles from
     * the delivery location back to the warehouse and traces it in the journal, all or nothing. 404 if the order is
     * unknown (or assigned to another driver for a driver); 409 if it is already delivered, failed or cancelled, or
     * if the delivery location does not hold the articles; 422 if a stock location is unknown or inactive.
     */
    DeliveryOrderRDto reportFailure(UUID orderId, DeliveryFailureWDto request);
}
