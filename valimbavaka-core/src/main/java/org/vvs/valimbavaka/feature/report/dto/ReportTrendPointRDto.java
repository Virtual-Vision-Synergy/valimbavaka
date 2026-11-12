package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * One point of the turnover curve (RPT-03): a business day, or the first day of a month for a yearly report, and
 * its turnover (0 when nothing was sold). The fields marked {@code @ExcelColumn} are those of the export (RPT-06).
 */
public record ReportTrendPointRDto(
        @ExcelColumn(header = "Date", order = 1, width = 14) LocalDate date,
        @ExcelColumn(header = "CA (Ar)", order = 2, format = "#,##0.00", width = 18) BigDecimal revenue) {}
