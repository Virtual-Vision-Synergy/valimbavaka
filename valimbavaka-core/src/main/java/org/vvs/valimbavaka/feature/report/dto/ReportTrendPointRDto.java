package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One point of the turnover curve (RPT-03): a business day, or the first day of a month for a yearly report, and
 * its turnover (0 when nothing was sold).
 */
public record ReportTrendPointRDto(LocalDate date, BigDecimal revenue) {}
