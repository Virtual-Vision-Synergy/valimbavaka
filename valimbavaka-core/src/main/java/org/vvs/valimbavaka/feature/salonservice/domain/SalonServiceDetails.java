package org.vvs.valimbavaka.feature.salonservice.domain;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Editable fields of a salon service: name, duration in minutes, price, optional commission rate in percent (none =
 * the channel rule, then the seller's rate applies) and products usually consumed.
 */
public record SalonServiceDetails(
        String name,
        int durationMinutes,
        BigDecimal price,
        @Nullable BigDecimal commissionRate,
        List<ConsumedProduct> products) {}
