package org.vvs.valimbavaka.feature.dashboard.service;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenueTrendRDto;

/**
 * Turnover of the last seven days for the cockpit (DSH-02). Like {@link DashboardService}, it computes nothing of its
 * own: each point is a daily read of the sales report.
 */
public interface RevenueTrendService {

    /** One point per day for the 7 days ending today in Madagascar time (Indian/Antananarivo), oldest first. */
    RevenueTrendRDto getLastSevenDays();
}
