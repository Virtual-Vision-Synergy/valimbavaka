package org.vvs.valimbavaka.feature.report.service;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;

/** Daily, monthly and yearly report for the administrator (RPT-01), filterable by channel and product range. */
public interface ReportService {

    /**
     * Report of the day, month or year that contains {@code date}. Defaults: month, today (Indian/Antananarivo).
     * {@code BusinessException} (422) if the period has not begun yet.
     */
    ReportRDto getReport(
            @Nullable ReportGranularity granularity,
            @Nullable LocalDate date,
            @Nullable UUID salesChannelId,
            @Nullable UUID rangeId);
}
