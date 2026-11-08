package org.vvs.valimbavaka.shared.excel;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.excel.exception.ExcelRowLimitExceededException;

/**
 * Lignes d'un tableau PDF, déjà mises en texte et nettoyées, dans la limite de {@code maxRows}. Une colonne
 * dont toutes les valeurs sont des nombres est alignée à droite.
 */
final class PdfRows {

    private final List<ExcelColumnDefinition> columns;
    private final PdfCellFormatter formatter;
    private final PdfText text;
    private final int maxRows;
    private final List<List<String>> rows = new ArrayList<>();
    private final boolean[] textual;

    PdfRows(List<ExcelColumnDefinition> columns, PdfCellFormatter formatter, PdfText text, int maxRows) {
        this.columns = columns;
        this.formatter = formatter;
        this.text = text;
        this.maxRows = maxRows;
        this.textual = new boolean[columns.size()];
    }

    /** Refuse la ligne au-delà du plafond : le document n'est pas encore écrit, le client reçoit un 422. */
    void add(Object item) {
        if (rows.size() >= maxRows) {
            throw new ExcelRowLimitExceededException(maxRows);
        }
        List<String> row = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            ExcelColumnDefinition column = columns.get(i);
            @Nullable Object value = column.valueOf(item);
            if (value != null && !(value instanceof Number)) {
                textual[i] = true;
            }
            row.add(text.sanitize(formatter.format(value, column.format())));
        }
        rows.add(row);
    }

    List<String> headers() {
        return columns.stream().map(column -> text.sanitize(column.header())).toList();
    }

    List<List<String>> rows() {
        return rows;
    }

    int columnCount() {
        return columns.size();
    }

    boolean rightAligned(int column) {
        return !textual[column];
    }
}
