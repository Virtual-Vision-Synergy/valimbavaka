package org.vvs.valimbavaka.feature.sale.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Who, where and when of a sale, checked before it is recorded. */
public record SaleHeader(
        UUID businessDayId,
        LocalDate businessDate,
        UUID salesChannelId,
        UUID stockLocationId,
        UUID sellerId,
        PaymentMethod paymentMethod) {}
