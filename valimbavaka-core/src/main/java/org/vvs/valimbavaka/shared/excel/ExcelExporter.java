package org.vvs.valimbavaka.shared.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;

/**
 * Écrit des classeurs xlsx en streaming (seules {@value #ROW_WINDOW} lignes restent en mémoire) à partir des
 * champs {@code @ExcelColumn}, au plus {@code vvs.excel.max-rows} lignes par onglet. Utilisable directement
 * pour un classeur à plusieurs onglets ; les flux fournis sont consommés mais pas fermés, l'appelant en reste
 * responsable.
 */
@Component
public class ExcelExporter {

    private static final int ROW_WINDOW = 100;

    private final ExcelProperties properties;
    private final Map<Class<?>, List<ExcelColumnDefinition>> columnsByType = new ConcurrentHashMap<>();

    public ExcelExporter(ExcelProperties properties) {
        this.properties = properties;
    }

    /** Un onglet : type du DTO (colonnes {@code @ExcelColumn}), nom de l'onglet et données. */
    public record Sheet<T>(Class<T> type, String name, Stream<T> data) {}

    public static <T> Sheet<T> sheet(Class<T> type, String name, Stream<T> data) {
        return new Sheet<>(type, name, data);
    }

    public <T> void export(Class<T> type, String sheetName, Stream<T> data, OutputStream out) throws IOException {
        export(out, sheet(type, sheetName, data));
    }

    /** Écrit un classeur contenant un onglet par {@link Sheet}, dans l'ordre fourni. */
    public void export(OutputStream out, Sheet<?>... sheets) throws IOException {
        // close() supprime aussi les fichiers temporaires du streaming
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_WINDOW)) {
            workbook.setCompressTempFiles(true);
            ExcelCellWriter cellWriter = new ExcelCellWriter(workbook, properties);
            for (Sheet<?> sheet : sheets) {
                writeSheet(workbook, cellWriter, sheet);
            }
            workbook.write(out);
        }
    }

    private void writeSheet(SXSSFWorkbook workbook, ExcelCellWriter cellWriter, Sheet<?> source) {
        List<ExcelColumnDefinition> columns = columnsByType.computeIfAbsent(source.type(), ExcelColumnDefinition::scan);
        ExcelSheetWriter writer = new ExcelSheetWriter(
                workbook.createSheet(WorkbookUtil.createSafeSheetName(source.name())), columns, cellWriter, properties);
        writer.writeHeader();
        source.data().forEachOrdered(writer::writeRow);
        writer.applyColumnWidths();
    }
}
