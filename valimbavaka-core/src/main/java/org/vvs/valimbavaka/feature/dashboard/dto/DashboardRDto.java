package org.vvs.valimbavaka.feature.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.day.domain.DayStatus;

/**
 * Daily cockpit of today (DSH-01): the four key indicators on one screen. {@code status} is the status of today's
 * business day ({@code null} when it is not opened yet, in which case {@code stockGap} is {@code null} too) and
 * {@code generatedAt} the server time of the snapshot. Later cockpit blocks (revenue trend DSH-02, revenue split
 * DSH-04) are separate reads: they are not part of this aggregate.
 */
public record DashboardRDto(
        LocalDate businessDate,
        @Nullable DayStatus status,
        Instant generatedAt,
        RevenueKpiRDto revenue,
        SalesKpiRDto sales,
        DeliveryKpiRDto deliveries,
        @Nullable StockGapKpiRDto stockGap) {}
