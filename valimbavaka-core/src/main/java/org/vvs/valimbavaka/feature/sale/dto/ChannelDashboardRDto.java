package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sales dashboard by channel (VTE-08) over the business days {@code [from, to]}: the totals of the period and one
 * entry per channel, best turnover first (active channels without sale included, at zero).
 */
public record ChannelDashboardRDto(
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        long totalSalesCount,
        List<ChannelSalesRDto> channels) {}
