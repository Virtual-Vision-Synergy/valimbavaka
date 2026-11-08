package org.vvs.valimbavaka.feature.commission.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.dto.ApplicableCommissionRDto;

/**
 * Rate applicable to a sale (COM-01, used by COM-02). PO decision of 07/11/2026: the most precise level wins, rates
 * never add up: salon service rate, otherwise the active rule of the sales channel, otherwise the seller's own rate
 * (AUTH-03), otherwise zero.
 */
public interface CommissionRateResolver {

    /**
     * @param sellerId account that made the sale
     * @param salesChannelId channel of the sale
     * @param salonServiceId salon service sold, {@code null} for a product sale (level plugged in by SRV-01 through
     *         {@link SalonServiceCommissionRates})
     * @throws org.vvs.valimbavaka.core.exception.NotFoundException if the seller is needed and does not exist
     */
    ApplicableCommissionRDto resolve(UUID sellerId, UUID salesChannelId, @Nullable UUID salonServiceId);
}
