package org.vvs.valimbavaka.feature.dayoverview.service;

import org.vvs.valimbavaka.feature.dayoverview.dto.DayLiveRDto;

/**
 * Live view of today for the administrator (JR-03): sales, deliveries and salon services of the day in one read,
 * polled by the screen. Read only; it opens nothing and closes nothing (JR-04 to JR-06 do).
 */
public interface DayLiveService {

    /** Snapshot of today in Madagascar time (Indian/Antananarivo): indicators and the latest operations. */
    DayLiveRDto getLive();
}
