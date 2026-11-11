package org.vvs.valimbavaka.feature.dashboard.service;

import org.vvs.valimbavaka.feature.dashboard.dto.DashboardRDto;

/**
 * Daily cockpit for the administrator (DSH-01): a read-only feature that composes the existing sources (reports,
 * channel dashboard, deliveries of the day, closing reconciliation) without computing any aggregate of its own.
 */
public interface DashboardService {

    /** The four key indicators of today in Madagascar time (Indian/Antananarivo), compared with the day before. */
    DashboardRDto getDashboard();
}
