package org.vvs.valimbavaka.feature.report.dto;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/** One line of the first sheet of the export (RPT-06): what the report covers (kind, periods, channel, range). */
public record ReportInfoRowRDto(
        @ExcelColumn(header = "Information", order = 1, width = 24)
        String label,

        @ExcelColumn(header = "Valeur", order = 2, width = 48)
        String value) {}
