package org.vvs.valimbavaka.shared.excel;

import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;
import org.vvs.valimbavaka.shared.excel.exception.ExcelRowLimitExceededException;

/** Remplit un onglet ligne par ligne, dans la limite de {@code maxRows}, et mesure chaque colonne. */
final class ExcelSheetWriter {

    private static final int CHARACTER_WIDTH = 256;

    private final SXSSFSheet sheet;
    private final List<ExcelColumnDefinition> columns;
    private final ExcelCellWriter cellWriter;
    private final ExcelProperties properties;
    private final int[] maxLengths;
    private int nextRow;

    ExcelSheetWriter(
            SXSSFSheet sheet,
            List<ExcelColumnDefinition> columns,
            ExcelCellWriter cellWriter,
            ExcelProperties properties) {
        this.sheet = sheet;
        this.columns = columns;
        this.cellWriter = cellWriter;
        this.properties = properties;
        this.maxLengths = new int[columns.size()];
    }

    void writeHeader() {
        Row row = sheet.createRow(nextRow++);
        for (int i = 0; i < columns.size(); i++) {
            String header = columns.get(i).header();
            maxLengths[i] = header.length();
            cellWriter.writeHeader(row.createCell(i), header);
        }
    }

    /** Refuse la ligne au-delà du plafond : rien n'a encore été envoyé au client, qui reçoit un 422. */
    void writeRow(Object item) {
        if (nextRow > properties.maxRows()) {
            throw new ExcelRowLimitExceededException(properties.maxRows());
        }
        Row row = sheet.createRow(nextRow++);
        for (int i = 0; i < columns.size(); i++) {
            ExcelColumnDefinition column = columns.get(i);
            int length = cellWriter.write(row.createCell(i), column.valueOf(item), column.format());
            maxLengths[i] = Math.max(maxLengths[i], length);
        }
    }

    /**
     * Appelé après les lignes (le {@code <cols>} n'est écrit qu'à la fin) : la largeur de l'annotation est un
     * minimum, la plus longue valeur plus la marge l'emporte, dans la limite du plafond configuré.
     */
    void applyColumnWidths() {
        ExcelProperties.Column widths = properties.column();
        for (int i = 0; i < columns.size(); i++) {
            int characters = Math.max(columns.get(i).width(), maxLengths[i] + widths.padding());
            sheet.setColumnWidth(i, Math.min(characters, widths.maxWidth()) * CHARACTER_WIDTH);
        }
    }
}
