package org.vvs.valimbavaka.feature.delivery.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Filters of the order list; a {@code null} criterion means "all". A driver's round = driver + day. */
public record DeliveryOrderFilter(
        @Nullable UUID driverId, @Nullable LocalDate scheduledDate, @Nullable DeliveryStatus status) {

    /** Same filters restricted to the given driver (a driver only reads his own orders). */
    public DeliveryOrderFilter forDriver(UUID ownDriverId) {
        return new DeliveryOrderFilter(ownDriverId, scheduledDate, status);
    }
}
