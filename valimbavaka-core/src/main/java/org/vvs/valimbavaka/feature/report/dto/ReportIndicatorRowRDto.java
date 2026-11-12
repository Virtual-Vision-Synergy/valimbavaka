package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * One indicator of the "Synthèse" sheet of the export (RPT-06): its value over the period, over the previous period
 * and the variation in percent ({@code null} when the previous value is 0), as shown by the cards of the screen.
 */
public record ReportIndicatorRowRDto(
        @ExcelColumn(header = "Indicateur", order = 1, width = 28) String indicator,

        @ExcelColumn(header = "Période", order = 2, width = 18)
        BigDecimal current,

        @ExcelColumn(header = "Période précédente", order = 3, width = 22)
        BigDecimal previous,

        @ExcelColumn(header = "Variation (%)", order = 4, format = "0.0", width = 16) @Nullable
        BigDecimal variation) {}
