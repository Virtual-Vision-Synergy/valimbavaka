package org.vvs.valimbavaka.shared.excel.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;

import org.vvs.valimbavaka.shared.excel.PdfExporter;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;

/**
 * Sérialise en PDF tabulaire le corps d'une route {@code .../export/pdf} ; titre : {@code @ExcelExport.title},
 * sinon {@code sheet}, sinon le nom du DTO.
 */
public class PdfHttpMessageConverter extends ExportHttpMessageConverter {

    private static final String EXTENSION = ".pdf";

    private final PdfExporter exporter;

    public PdfHttpMessageConverter(PdfExporter exporter, ApplicationEventPublisher eventPublisher) {
        super(MediaType.APPLICATION_PDF, EXTENSION, eventPublisher);
        this.exporter = exporter;
    }

    @Override
    protected void writeFile(OutputStream out, Class<?> elementType, Stream<?> data, @Nullable ExcelExport annotation)
            throws IOException {
        exporter.export(report(elementType, title(elementType, annotation), data), out);
    }

    private static String title(Class<?> elementType, @Nullable ExcelExport annotation) {
        if (annotation == null) {
            return elementType.getSimpleName();
        }
        if (!annotation.title().isBlank()) {
            return annotation.title();
        }
        return annotation.sheet().isBlank() ? elementType.getSimpleName() : annotation.sheet();
    }

    // Typage sans cast non vérifié : chaque élément est converti vers le type déclaré
    private static <T> PdfExporter.Report<T> report(Class<T> type, String title, Stream<?> data) {
        return PdfExporter.report(title, type, data.map(type::cast));
    }
}
