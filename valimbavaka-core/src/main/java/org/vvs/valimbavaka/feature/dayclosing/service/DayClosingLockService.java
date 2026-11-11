package org.vvs.valimbavaka.feature.dayclosing.service;

import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;

/**
 * Lock of today's closing (JR-06): once the evening count is complete and every gap has its reason, the administrator
 * closes the day. The closing is then frozen on the count lines and nothing more is recorded for that date.
 */
public interface DayClosingLockService {

    /**
     * Rereads the reconciliation of today (a sale after the count changes the theoretical quantity), checks that every
     * line is counted and every gap justified, freezes the count lines on the theoretical quantity of the lock,
     * closes the day and traces the closing, with its gaps, in the journal. All or nothing; returns the reconciliation
     * of the closed day.
     *
     * @throws org.vvs.valimbavaka.core.exception.ConflictException 409 if today's day is not open (not opened,
     *     already closed, or closed by a simultaneous request)
     * @throws org.vvs.valimbavaka.core.exception.BusinessException 422 if a line is not counted or a gap has no reason
     */
    DayClosingRDto lock();
}
