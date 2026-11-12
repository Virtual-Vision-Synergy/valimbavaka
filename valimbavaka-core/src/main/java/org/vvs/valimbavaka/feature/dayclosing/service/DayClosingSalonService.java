package org.vvs.valimbavaka.feature.dayclosing.service;

import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingSalonRDto;

/**
 * Salon consumption of today for the administrator (SRV-04): what the performed services consumed, article by
 * article, next to the « Salon » column of the closing reconciliation (JR-04). Read only.
 */
public interface DayClosingSalonService {

    /** Same day and same 409 as {@link DayClosingService#getClosing()}: today's day must be opened. */
    ClosingSalonRDto getSalonConsumption();
}
