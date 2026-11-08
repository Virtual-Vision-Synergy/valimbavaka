package org.vvs.valimbavaka.feature.commission.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Extension point for the salon service level (SRV-01): the services feature provides one Spring bean implementing
 * this port. Without any bean, the resolver skips this level and falls back to the channel, then the seller.
 */
public interface SalonServiceCommissionRates {

    /** Commission rate in percent (0 to 100, two decimals) of the salon service, empty if it defines none. */
    Optional<BigDecimal> findRate(UUID salonServiceId);
}
