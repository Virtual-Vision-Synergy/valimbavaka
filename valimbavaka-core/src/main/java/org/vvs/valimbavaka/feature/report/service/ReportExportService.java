package org.vvs.valimbavaka.feature.report.service;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;

/**
 * Export of the report in xlsx and PDF (RPT-06), administrator only. The figures are those of {@link ReportService}:
 * the files and the screen never come from two different calculations. Same parameters and defaults as the report;
 * {@code BusinessException} (422) if the period has not begun yet.
 */
public interface ReportExportService {

    /** Workbook with one sheet per block of the report: information, summary, curve, ranges and ranking. */
    ReportFile exportXlsx(
            @Nullable ReportGranularity granularity,
            @Nullable LocalDate date,
            @Nullable UUID salesChannelId,
            @Nullable UUID rangeId);

    /** One PDF: filters and indicators under the title, then one table (curve, ranges, ranking). */
    ReportFile exportPdf(
            @Nullable ReportGranularity granularity,
            @Nullable LocalDate date,
            @Nullable UUID salesChannelId,
            @Nullable UUID rangeId);
}
