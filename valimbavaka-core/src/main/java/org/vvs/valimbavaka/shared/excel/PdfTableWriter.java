package org.vvs.valimbavaka.shared.excel;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jspecify.annotations.Nullable;

/**
 * Met en page un tableau sur des pages A4 paysage : titre et lignes d'information en tête, en-tête du tableau
 * répété sur chaque page, résumé après la dernière ligne, puis pied « Généré le … · Page x / n » sur chaque page.
 * Les colonnes prennent la largeur de leur plus long texte (plafonnée), réduite à la largeur de la page.
 */
final class PdfTableWriter {

    private static final PDRectangle PAGE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MARGIN = 36;
    private static final float FOOTER_BASELINE = 20;
    private static final float ROW_HEIGHT = 14;
    private static final float TITLE_HEIGHT = 22;
    private static final float TEXT_OFFSET = 4;
    private static final float CELL_PADDING = 4;
    private static final float MAX_COLUMN_WIDTH = 220;
    private static final float HEADER_GREY = 0.9f;
    private static final float LINE_GREY = 0.75f;
    private static final float LINE_WIDTH = 0.5f;
    private static final String EMPTY_TABLE = "Aucune ligne à exporter.";

    private final PDDocument document;
    private final PdfText text;
    private final PdfRows rows;
    private final float[] widths;
    private final float tableWidth;
    private @Nullable PDPageContentStream content;
    private float y;

    PdfTableWriter(PDDocument document, PdfText text, PdfRows rows) throws IOException {
        this.document = document;
        this.text = text;
        this.rows = rows;
        this.widths = columnWidths(text, rows);
        float total = 0;
        for (float width : widths) {
            total += width;
        }
        this.tableWidth = total;
    }

    void write(PdfExporter.Report<?> report) throws IOException {
        try {
            newPage();
            writeLine(report.title(), text.title(), TITLE_HEIGHT);
            for (String detail : report.details()) {
                writeLine(detail, text.regular(), ROW_HEIGHT);
            }
            y -= ROW_HEIGHT / 2;
            writeTable();
            y -= ROW_HEIGHT / 2;
            for (String line : report.summary()) {
                writeLine(line, text.bold(), ROW_HEIGHT);
            }
        } finally {
            closeContent();
        }
    }

    /** Second passage, une fois le nombre de pages connu. */
    void writeFooters(String generatedAt) throws IOException {
        int total = document.getNumberOfPages();
        int number = 1;
        for (PDPage page : document.getPages()) {
            try (PDPageContentStream footer =
                    new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                String label = "Généré le " + generatedAt + " · Page " + number + " / " + total;
                PdfText.show(footer, text.regular(), text.sanitize(label), new PdfText.At(MARGIN, FOOTER_BASELINE));
            }
            number++;
        }
    }

    private void writeTable() throws IOException {
        ensureSpace(2 * ROW_HEIGHT);
        writeHeaderRow();
        if (rows.rows().isEmpty()) {
            writeLine(EMPTY_TABLE, text.regular(), ROW_HEIGHT);
        }
        for (List<String> row : rows.rows()) {
            if (y - ROW_HEIGHT < MARGIN) {
                newPage();
                writeHeaderRow();
            }
            writeCells(row, text.regular());
            drawRowSeparator();
        }
    }

    private void writeHeaderRow() throws IOException {
        PDPageContentStream stream = content();
        stream.setNonStrokingColor(HEADER_GREY, HEADER_GREY, HEADER_GREY);
        stream.addRect(MARGIN, y - ROW_HEIGHT, tableWidth, ROW_HEIGHT);
        stream.fill();
        stream.setNonStrokingColor(0f, 0f, 0f);
        writeCells(rows.headers(), text.bold());
    }

    private void writeCells(List<String> cells, PdfText.Style style) throws IOException {
        float x = MARGIN;
        float baseline = y - ROW_HEIGHT + TEXT_OFFSET;
        for (int i = 0; i < cells.size(); i++) {
            String cell = text.fit(style, cells.get(i), widths[i] - 2 * CELL_PADDING);
            float offset = rows.rightAligned(i) ? widths[i] - CELL_PADDING - text.width(style, cell) : CELL_PADDING;
            PdfText.show(content(), style, cell, new PdfText.At(x + offset, baseline));
            x += widths[i];
        }
        y -= ROW_HEIGHT;
    }

    private void drawRowSeparator() throws IOException {
        PDPageContentStream stream = content();
        stream.setStrokingColor(LINE_GREY, LINE_GREY, LINE_GREY);
        stream.setLineWidth(LINE_WIDTH);
        stream.moveTo(MARGIN, y);
        stream.lineTo(MARGIN + tableWidth, y);
        stream.stroke();
    }

    private void writeLine(String value, PdfText.Style style, float height) throws IOException {
        ensureSpace(height);
        String line = text.fit(style, text.sanitize(value), PAGE.getWidth() - 2 * MARGIN);
        PdfText.show(content(), style, line, new PdfText.At(MARGIN, y - height + TEXT_OFFSET));
        y -= height;
    }

    private void ensureSpace(float height) throws IOException {
        if (y - height < MARGIN) {
            newPage();
        }
    }

    private void newPage() throws IOException {
        closeContent();
        PDPage page = new PDPage(PAGE);
        document.addPage(page);
        content = new PDPageContentStream(document, page);
        y = PAGE.getHeight() - MARGIN;
    }

    private PDPageContentStream content() {
        if (content == null) {
            throw new IllegalStateException("Aucune page ouverte");
        }
        return content;
    }

    private void closeContent() throws IOException {
        if (content != null) {
            content.close();
            content = null;
        }
    }

    private static float[] columnWidths(PdfText text, PdfRows rows) throws IOException {
        float[] widths = new float[rows.columnCount()];
        List<String> headers = rows.headers();
        for (int i = 0; i < widths.length; i++) {
            widths[i] = text.width(text.bold(), headers.get(i));
        }
        for (List<String> row : rows.rows()) {
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(widths[i], text.width(text.regular(), row.get(i)));
            }
        }
        float total = 0;
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Math.min(widths[i] + 2 * CELL_PADDING, MAX_COLUMN_WIDTH);
            total += widths[i];
        }
        float scale = Math.min(1f, (PAGE.getWidth() - 2 * MARGIN) / total);
        for (int i = 0; i < widths.length; i++) {
            widths[i] *= scale;
        }
        return widths;
    }
}
