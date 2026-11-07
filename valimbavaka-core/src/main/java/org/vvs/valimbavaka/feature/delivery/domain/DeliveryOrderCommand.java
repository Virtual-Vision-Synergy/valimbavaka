package org.vvs.valimbavaka.feature.delivery.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Checked content of a new order to deliver: customer, assigned driver, day of the round, note and priced lines. */
public record DeliveryOrderCommand(
        DeliveryCustomer customer,
        UUID driverId,
        LocalDate scheduledDate,
        @Nullable String note,
        List<DeliveryOrderLineCommand> lines) {}
