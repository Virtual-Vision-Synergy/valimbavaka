package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * One channel of the dashboard (VTE-08): turnover, number of validated sales and share of the total turnover in
 * percent (one decimal, 0 when nothing was sold). The label is {@code null} for a channel deactivated since.
 */
public record ChannelSalesRDto(
        UUID salesChannelId,
        @Nullable String salesChannelLabel,
        BigDecimal revenue,
        long salesCount,
        BigDecimal revenueSharePercent) {}
