package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.time.LocalDate;
import java.util.List;

import org.vvs.valimbavaka.feature.day.domain.DayStatus;

/** Closing reconciliation of today (JR-04): one block per location, in the order store, delivery, salon. */
public record DayClosingRDto(LocalDate businessDate, DayStatus status, List<ClosingLocationRDto> locations) {}
