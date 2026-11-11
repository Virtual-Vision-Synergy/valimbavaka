package org.vvs.valimbavaka.feature.dayclosing.service;

import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;

/**
 * Closing reconciliation of today for the administrator (JR-04): for each location and article, what was
 * transferred, returned, sold, delivered, consumed by the salon and taken for a shooting, and the theoretical
 * quantity left. Read only; the counts, the reasons and the lock come with JR-05 and JR-06.
 */
public interface DayClosingService {

    /**
     * Reconciliation of today in Madagascar time (Indian/Antananarivo). 409 if today's day is not opened; a closed
     * day can still be read.
     */
    DayClosingRDto getClosing();
}
