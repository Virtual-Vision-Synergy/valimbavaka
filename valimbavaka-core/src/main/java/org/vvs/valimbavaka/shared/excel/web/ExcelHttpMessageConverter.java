package org.vvs.valimbavaka.shared.excel.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.shared.excel.ExcelExporter;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;

/** Sérialise en xlsx le corps d'une route {@code .../export} (un onglet nommé par {@code @ExcelExport.sheet}). */
public class ExcelHttpMessageConverter extends ExportHttpMessageConverter {

    public static final String XLSX_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final MediaType XLSX = MediaType.parseMediaType(XLSX_VALUE);

    private static final String EXTENSION = ".xlsx";

    private final ExcelExporter exporter;

    public ExcelHttpMessageConverter(ExcelExporter exporter, ApplicationEventPublisher eventPublisher) {
        super(XLSX, EXTENSION, eventPublisher);
        this.exporter = exporter;
    }

    @Override
    protected void writeFile(OutputStream out, Class<?> elementType, Stream<?> data, @Nullable ExcelExport annotation)
            throws IOException {
        String sheetName =
                annotation == null || annotation.sheet().isBlank() ? elementType.getSimpleName() : annotation.sheet();
        exporter.export(out, sheet(elementType, sheetName, data));
    }

    // Typage sans cast non vérifié : chaque élément est converti vers le type déclaré
    private static <T> ExcelExporter.Sheet<T> sheet(Class<T> type, String name, Stream<?> data) {
        return ExcelExporter.sheet(type, name, data.map(type::cast));
    }
}
