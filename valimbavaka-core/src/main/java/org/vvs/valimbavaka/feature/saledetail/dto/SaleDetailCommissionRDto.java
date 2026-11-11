package org.vvs.valimbavaka.feature.saledetail.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;

/**
 * Commission computed on a sale (COM-02) and the rule applied: {@code rateSource} is the level the rate comes from
 * (channel rule, seller's own rate, none), {@code ruleId} and {@code ruleName} identify the channel rule when it is the
 * source, {@code base} says whether the rate applied to the revenue or to the margin.
 */
public record SaleDetailCommissionRDto(
        UUID id,
        BigDecimal revenue,
        BigDecimal baseAmount,
        CommissionBase base,
        BigDecimal rate,
        CommissionRateSource rateSource,
        @Nullable UUID ruleId,
        @Nullable String ruleName,
        BigDecimal amount,
        CommissionStatus status) {}
