package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * Turnover of one product range over a period (RPT-04): the sum of the line totals of its articles (before the
 * discount of the whole sale) and its share of the turnover of all the ranges, in percent with one decimal. The
 * fields marked {@code @ExcelColumn} are those of the export (RPT-06); the identifier stays out of the files.
 */
public record ReportRangeRDto(
        UUID rangeId,
        @ExcelColumn(header = "Code", order = 1, width = 14) String code,

        @ExcelColumn(header = "Gamme", order = 2, width = 30)
        String label,

        @ExcelColumn(header = "CA (Ar)", order = 3, format = "#,##0.00", width = 18)
        BigDecimal revenue,

        @ExcelColumn(header = "Part (%)", order = 4, format = "0.0", width = 12)
        BigDecimal share) {}
