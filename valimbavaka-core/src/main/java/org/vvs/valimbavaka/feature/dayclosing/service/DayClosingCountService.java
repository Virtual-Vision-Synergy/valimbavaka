package org.vvs.valimbavaka.feature.dayclosing.service;

import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingCountsWDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;

/**
 * Evening count of today's closing (JR-05): the administrator records the counted quantity of each article and, for
 * every gap with the theoretical quantity, the reason. The reason is checked here, not only on screen.
 */
public interface DayClosingCountService {

    /**
     * Saves the counts of today (a new count of a line replaces the previous one) and returns the reconciliation
     * with them. All or nothing: the theoretical quantity of each line is frozen when saved.
     *
     * @throws org.vvs.valimbavaka.core.exception.NotFoundException 404 if a line is not part of today's closing
     * @throws org.vvs.valimbavaka.core.exception.ConflictException 409 if today's day is not open (not opened or
     *     already closed)
     * @throws org.vvs.valimbavaka.core.exception.BusinessException 422 if a line has a gap without a reason, or is
     *     sent twice
     */
    DayClosingRDto saveCounts(DayClosingCountsWDto request);
}
