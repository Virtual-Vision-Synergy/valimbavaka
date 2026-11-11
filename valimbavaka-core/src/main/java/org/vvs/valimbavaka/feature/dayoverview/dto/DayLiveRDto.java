package org.vvs.valimbavaka.feature.dayoverview.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.day.domain.DayStatus;

/**
 * Live view of today (JR-03): {@code status} of the business day ({@code null} when it is not opened yet), the
 * indicators of the three activities and the {@code feed} of the latest operations, most recent first.
 * {@code generatedAt} is the server time of the snapshot.
 */
public record DayLiveRDto(
        LocalDate businessDate,
        @Nullable DayStatus status,
        Instant generatedAt,
        DaySalesRDto sales,
        DayDeliveriesRDto deliveries,
        DayServicesRDto services,
        List<DayFeedItemRDto> feed) {}
