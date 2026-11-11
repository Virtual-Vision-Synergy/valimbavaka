package org.vvs.valimbavaka.feature.sale.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Filters of the day's sales list (VTE-07): one business day, optionally one channel and one seller; a {@code null}
 * criterion means "all" (a {@code null} day is resolved to today by the service).
 */
public record SaleFilter(
        @Nullable LocalDate businessDate,
        @Nullable UUID salesChannelId,
        @Nullable UUID sellerId) {

    /** Same filters restricted to the given seller (a seller only reads the sales credited to her). */
    public SaleFilter forSeller(UUID ownSellerId) {
        return new SaleFilter(businessDate, salesChannelId, ownSellerId);
    }
}
