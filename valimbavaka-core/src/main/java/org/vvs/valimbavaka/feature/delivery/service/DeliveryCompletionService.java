package org.vvs.valimbavaka.feature.delivery.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;

/**
 * Delivery marked as delivered by the driver in one gesture (LIV-03): the order becomes a sale of the delivery
 * channel, which FAC-01 turns into an invoice (decision of the product owner, 07/11/2026).
 */
public interface DeliveryCompletionService {

    /**
     * Moves the order to {@code DELIVERED} without any further input, records the sale of the delivery channel at the
     * prices of the order (stock taken out of the delivery location, paid on delivery, credited to the seller who
     * entered the order) and traces it in the journal, all or nothing. 404 if the order is unknown (or assigned to
     * another driver for a driver); 409 if it is already delivered, failed or cancelled, if today is not opened or if
     * the delivery location lacks the articles; 422 if the delivery channel is unknown or inactive.
     */
    DeliveryOrderRDto markDelivered(UUID orderId);
}
