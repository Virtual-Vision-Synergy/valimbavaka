package org.vvs.valimbavaka.shared.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;

/**
 * Écrit un PDF tabulaire (A4 paysage) à partir des mêmes champs {@code @ExcelColumn} que l'export xlsx, au plus
 * {@code vvs.excel.max-rows} lignes. Le document est construit en mémoire puis écrit d'un bloc : une erreur
 * (plafond dépassé, expression invalide) n'envoie aucun octet. Le flux fourni est consommé mais pas fermé.
 */
@Component
public class PdfExporter {

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ExcelProperties properties;
    private final Clock clock;
    private final Map<Class<?>, List<ExcelColumnDefinition>> columnsByType = new ConcurrentHashMap<>();

    public PdfExporter(ExcelProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Un document : titre, lignes d'information sous le titre (client, période, filtres…), tableau du type
     * {@code type} (colonnes {@code @ExcelColumn}), puis lignes de résumé après le tableau (totaux…).
     */
    public record Report<T>(String title, List<String> details, Class<T> type, Stream<T> data, List<String> summary) {

        public Report {
            details = List.copyOf(details);
            summary = List.copyOf(summary);
        }

        public Report<T> withDetails(List<String> lines) {
            return new Report<>(title, lines, type, data, summary);
        }

        public Report<T> withSummary(List<String> lines) {
            return new Report<>(title, details, type, data, lines);
        }
    }

    public static <T> Report<T> report(String title, Class<T> type, Stream<T> data) {
        return new Report<>(title, List.of(), type, data, List.of());
    }

    public <T> void export(Class<T> type, String title, Stream<T> data, OutputStream out) throws IOException {
        export(report(title, type, data), out);
    }

    public void export(Report<?> report, OutputStream out) throws IOException {
        PdfText text = new PdfText();
        PdfRows rows = readRows(report, text);
        try (PDDocument document = new PDDocument()) {
            document.getDocumentInformation().setTitle(report.title());
            PdfTableWriter writer = new PdfTableWriter(document, text, rows);
            writer.write(report);
            writer.writeFooters(GENERATED_AT.format(LocalDateTime.ofInstant(clock.instant(), properties.timeZone())));
            document.save(out);
        }
    }

    private PdfRows readRows(Report<?> report, PdfText text) {
        List<ExcelColumnDefinition> columns = columnsByType.computeIfAbsent(report.type(), ExcelColumnDefinition::scan);
        PdfRows rows = new PdfRows(columns, new PdfCellFormatter(properties.timeZone()), text, properties.maxRows());
        report.data().forEachOrdered(rows::add);
        return rows;
    }
}
