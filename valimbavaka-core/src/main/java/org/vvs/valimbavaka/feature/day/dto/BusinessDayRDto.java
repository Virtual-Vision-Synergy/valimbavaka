package org.vvs.valimbavaka.feature.day.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.day.domain.DayStatus;

/**
 * Business day: {@code createdAt} is the opening time, {@code createdBy} the user who opened it; {@code transfers}
 * are the morning transfers, one per destination, in the order they were generated.
 */
public record BusinessDayRDto(
        UUID id,
        LocalDate businessDate,
        DayStatus status,
        List<DayTransferRDto> transfers,
        Instant createdAt,
        @Nullable UUID createdBy) {}
