package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.day.domain.DayStatus;

/**
 * Closing reconciliation of today (JR-04): one block per location, in the order store, delivery, salon. {@code
 * businessDayId} identifies the day the counts (JR-05) belong to.
 */
public record DayClosingRDto(
        UUID businessDayId, LocalDate businessDate, DayStatus status, List<ClosingLocationRDto> locations) {}
