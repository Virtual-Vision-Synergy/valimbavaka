package org.vvs.valimbavaka.feature.report.dto;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;

/**
 * Report of a period (RPT-01): the effective period {@code [from, to]}, the filters applied (null = all) and the
 * sales indicators and their comparison with the previous period (RPT-02). The later reports add their sections here
 * (curve, ranges, ranking).
 */
public record ReportRDto(
        ReportGranularity granularity,
        LocalDate from,
        LocalDate to,
        @Nullable UUID salesChannelId,
        @Nullable UUID rangeId,
        ReportSalesRDto sales,
        ReportComparisonRDto comparison) {}
