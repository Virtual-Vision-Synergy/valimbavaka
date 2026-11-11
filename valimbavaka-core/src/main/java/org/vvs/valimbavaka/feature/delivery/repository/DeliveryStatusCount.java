package org.vvs.valimbavaka.feature.delivery.repository;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;

/** Number of orders to deliver in one status over a day (JR-03). */
public record DeliveryStatusCount(DeliveryStatus status, long total) {}
