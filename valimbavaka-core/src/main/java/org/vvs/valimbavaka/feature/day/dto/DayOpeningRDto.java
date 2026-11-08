package org.vvs.valimbavaka.feature.day.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;

/**
 * Morning dispatch grid of today: the articles available at the warehouse and the destinations (every active
 * location but the warehouse). {@code day} is set when today is already opened; {@code previousDay} is the last
 * day opened before today (null for the very first one).
 */
public record DayOpeningRDto(
        LocalDate businessDate,
        @Nullable LocalDate previousDay,
        @Nullable BusinessDayRDto day,
        UUID warehouseLocationId,
        List<StockLocationRDto> destinations,
        List<DayOpeningLineRDto> lines) {}
