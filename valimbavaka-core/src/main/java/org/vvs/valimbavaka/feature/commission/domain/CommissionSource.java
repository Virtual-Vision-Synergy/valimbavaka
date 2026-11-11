package org.vvs.valimbavaka.feature.commission.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Operation to compute a commission on (COM-02), given by the feature that recorded it.
 *
 * @param type sale or salon service performed
 * @param id identifier of the sale or of the service record (one commission per operation)
 * @param number number shown to the users ({@code VTE-0042}, {@code PRE-0007})
 * @param sellerId account credited with the operation
 * @param salesChannelId channel of the operation (the store for a salon service)
 * @param salonServiceId salon service performed, {@code null} for a sale of products
 * @param businessDate business day of the operation (month of the commission)
 * @param revenue revenue of the operation after discounts (sale total, service price)
 * @param purchaseCost purchase cost of the goods sold (margin base); {@code null} when unknown, then the rate applies
 *         to the revenue
 */
public record CommissionSource(
        CommissionSourceType type,
        UUID id,
        String number,
        UUID sellerId,
        UUID salesChannelId,
        @Nullable UUID salonServiceId,
        LocalDate businessDate,
        BigDecimal revenue,
        @Nullable BigDecimal purchaseCost) {}
