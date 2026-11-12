package org.vvs.valimbavaka.feature.dashboard.service;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenueSplitRDto;

/**
 * Turnover of the day by product range and by sales channel for the cockpit (DSH-04). Like {@link DashboardService}, it
 * computes no turnover of its own: ranges come from the sales report, channels from the channel dashboard.
 */
public interface RevenueSplitService {

    /** Split of today (Madagascar time, Indian/Antananarivo): validated sales only, 0 for a range without sale. */
    RevenueSplitRDto getToday();
}
