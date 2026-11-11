package org.vvs.valimbavaka.feature.day.service;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningWDto;

/**
 * Business day (JR-01): opened once a day by the morning dispatch, it is the frame of the day's operations. The
 * date is always today's in the business time zone (Indian/Antananarivo); {@link #requireOpenDay} is the port of
 * the features that need an opened day (sales VTE-01, deliveries, salon services).
 */
public interface BusinessDayService {

    /** Dispatch grid of today: articles available at the warehouse, destinations, and today's day if opened. */
    DayOpeningRDto getOpening();

    /**
     * Opens today: records one morning transfer (origin {@code MORNING}, status {@code VALIDATED}) per destination
     * from the warehouse, noted {@code Ouverture de la journée du 08/11/2026} (TRF-02), moves the stock and traces
     * the opening in the journal, all or nothing. 409 if today is already opened or the warehouse does not hold
     * enough stock; 422 if a destination is the warehouse, unknown or inactive, or holds more than 100 articles; 404
     * if an article is unknown.
     */
    BusinessDayRDto openDay(DayOpeningWDto request);

    /** Today's business day whatever its status (JR-03), {@code null} when today is not opened yet. */
    @Nullable
    BusinessDayRDto findToday();

    /** Today's business day; 409 (CONFLICT) if today is not opened yet or already closed. */
    BusinessDayRDto requireOpenDay();

    /**
     * Today's open day, locked in writing until the end of the transaction (JR-06): the closing and the saving of
     * the evening counts of the day run one after the other. 409 (CONFLICT) if today is not opened or already closed.
     */
    BusinessDayRDto lockOpenDay();

    /**
     * Closes today's day (JR-06): from then on {@link #requireOpenDay} refuses, so no sale, cancellation, credit
     * note or count is recorded for that date. The checks of the evening and the journal line belong to the caller.
     * 409 (CONFLICT) if today is not opened or already closed (two simultaneous closings: the second one fails).
     */
    BusinessDayRDto closeDay();
}
