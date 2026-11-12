package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * One row of the ranking of the best-selling articles (RPT-05): quantity sold (3 decimals), turnover (sum of the line
 * totals, before the discount of the whole sale), gross margin (turnover minus purchase cost, {@code null} when the
 * cost of a line was unknown when it was sold) and share of the turnover of all the lines of the period, in percent
 * with one decimal. The fields marked {@code @ExcelColumn} are those of the export (RPT-06).
 */
public record ReportRankedArticleRDto(
        UUID articleId,
        @ExcelColumn(header = "Code", order = 1, width = 14) String code,
        @ExcelColumn(header = "Article", order = 2, width = 36) String name,
        @ExcelColumn(header = "Quantité", order = 3, width = 12) BigDecimal quantity,
        @ExcelColumn(header = "CA (Ar)", order = 4, format = "#,##0.00", width = 18) BigDecimal revenue,
        @ExcelColumn(header = "Marge (Ar)", order = 5, format = "#,##0.00", width = 18) @Nullable BigDecimal margin,
        @ExcelColumn(header = "Part (%)", order = 6, format = "0.0", width = 12) BigDecimal share) {}
