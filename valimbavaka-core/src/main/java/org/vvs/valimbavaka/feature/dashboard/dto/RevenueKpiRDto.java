package org.vvs.valimbavaka.feature.dashboard.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Turnover of today (validated sales) against the day before. {@code changePercent} is the evolution in percent
 * with one decimal, {@code null} when the day before had no turnover (nothing to compare with).
 */
public record RevenueKpiRDto(
        BigDecimal amount,
        BigDecimal previousAmount,
        @Nullable BigDecimal changePercent) {}
