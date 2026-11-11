package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;

/**
 * Commission computed on a sale or a salon service (COM-02): {@code amount} = {@code baseAmount} × {@code rate} / 100,
 * rounded half up to the ariary. {@code base} is the base actually applied ({@code REVENUE} or {@code MARGIN}),
 * {@code rateSource} the level the rate comes from and {@code ruleId} the channel rule applied, if any.
 */
public record CommissionRDto(
        UUID id,
        CommissionSourceType sourceType,
        UUID sourceId,
        String sourceNumber,
        UUID sellerId,
        String sellerName,
        UUID salesChannelId,
        @Nullable UUID salonServiceId,
        LocalDate businessDate,
        BigDecimal revenue,
        BigDecimal baseAmount,
        CommissionBase base,
        BigDecimal rate,
        CommissionRateSource rateSource,
        @Nullable UUID ruleId,
        BigDecimal amount,
        CommissionStatus status,
        Instant createdAt) {}
