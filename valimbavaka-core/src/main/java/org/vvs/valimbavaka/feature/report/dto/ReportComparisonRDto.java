package org.vvs.valimbavaka.feature.report.dto;

import java.time.LocalDate;

/**
 * Comparison with the previous period (RPT-02): the previous period {@code [from, to]} (the day, month or year before,
 * same channel and range filters), its sales indicators and the variation of each indicator in percent.
 */
public record ReportComparisonRDto(
        LocalDate from, LocalDate to, ReportSalesRDto sales, ReportVariationRDto variation) {}
