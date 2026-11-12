package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * One row of the single table of the PDF export (RPT-06): the PDF of SYS-01 has one table, so the turnover curve, the
 * ranges and the ranking follow one another in it, each row naming its block. Empty cells for what a block lacks.
 */
public record ReportPdfRowRDto(
        @ExcelColumn(header = "Bloc", order = 1, width = 14) String section,

        @ExcelColumn(header = "Libellé", order = 2, width = 40)
        String label,

        @ExcelColumn(header = "Quantité", order = 3, width = 12) @Nullable
        BigDecimal quantity,

        @ExcelColumn(header = "CA (Ar)", order = 4, format = "#,##0.00", width = 18)
        BigDecimal revenue,

        @ExcelColumn(header = "Marge (Ar)", order = 5, format = "#,##0.00", width = 18) @Nullable
        BigDecimal margin,

        @ExcelColumn(header = "Part (%)", order = 6, format = "0.0", width = 12) @Nullable
        BigDecimal share) {}
