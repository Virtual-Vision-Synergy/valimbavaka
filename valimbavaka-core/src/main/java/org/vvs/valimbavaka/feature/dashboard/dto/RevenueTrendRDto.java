package org.vvs.valimbavaka.feature.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Revenue trend of the dashboard (DSH-02), computed in the business time zone (Indian/Antananarivo).
 *
 * @param today current business date, last point of {@code last7Days}
 * @param last7Days one point per day from {@code today - 6} to {@code today}, oldest first, days without revenue at 0
 * @param last4Weeks one point per week (Monday to Sunday) for the 4 last weeks, the current one last
 */
public record RevenueTrendRDto(LocalDate today, List<RevenuePointRDto> last7Days, List<RevenuePointRDto> last4Weeks) {}
