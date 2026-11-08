package org.vvs.valimbavaka.feature.dashboard.service;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenueTrendRDto;

/** Read-only figures of the dashboard (DSH-02). */
public interface DashboardService {

    /**
     * Revenue of the last 7 days and of the last 4 weeks: validated sales (by business date) plus performed salon
     * services (by local date of performance). Days and weeks without revenue are returned at 0.
     */
    RevenueTrendRDto getRevenueTrend();
}
