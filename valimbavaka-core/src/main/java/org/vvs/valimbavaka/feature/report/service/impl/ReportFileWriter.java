package org.vvs.valimbavaka.feature.report.service.impl;

import static org.vvs.valimbavaka.shared.excel.ExcelExporter.sheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.report.dto.ReportIndicatorRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportInfoRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportPdfRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRangeRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRankedArticleRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportTrendPointRDto;
import org.vvs.valimbavaka.shared.excel.ExcelExporter;
import org.vvs.valimbavaka.shared.excel.PdfExporter;

/** Writes a report (RPT-06) as an xlsx workbook with five sheets, or as one PDF; the whole file is built in memory. */
@Component
class ReportFileWriter {

    private final ExcelExporter excelExporter;
    private final PdfExporter pdfExporter;
    private final ReportExportLayout layout;

    ReportFileWriter(ExcelExporter excelExporter, PdfExporter pdfExporter, ReportExportLayout layout) {
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
        this.layout = layout;
    }

    byte[] xlsx(ReportRDto report, ReportLabels labels) {
        var out = new ByteArrayOutputStream();
        try {
            excelExporter.export(
                    out,
                    sheet(ReportInfoRowRDto.class, "Rapport", layout.info(report, labels).stream()),
                    sheet(ReportIndicatorRowRDto.class, "Synthèse", layout.indicators(report).stream()),
                    sheet(ReportTrendPointRDto.class, "Évolution", report.trend().stream()),
                    sheet(ReportRangeRDto.class, "Gammes", report.ranges().items().stream()),
                    sheet(ReportRankedArticleRDto.class, "Classement", report.ranking().items().stream()));
        } catch (IOException e) {
            throw new UncheckedIOException("Classeur du rapport impossible", e);
        }
        return out.toByteArray();
    }

    byte[] pdf(ReportRDto report, ReportLabels labels) {
        var out = new ByteArrayOutputStream();
        var document = PdfExporter.report(layout.title(report), ReportPdfRowRDto.class, layout.pdfRows(report).stream())
                .withDetails(layout.pdfDetails(report, labels));
        try {
            pdfExporter.export(document, out);
        } catch (IOException e) {
            throw new UncheckedIOException("PDF du rapport impossible", e);
        }
        return out.toByteArray();
    }
}
