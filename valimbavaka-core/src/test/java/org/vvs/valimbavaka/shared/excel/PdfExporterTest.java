package org.vvs.valimbavaka.shared.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;
import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;
import org.vvs.valimbavaka.shared.excel.exception.ExcelRowLimitExceededException;

class PdfExporterTest {

    private static final int MAX_ROWS = 200;
    private static final Instant NOW = Instant.parse("2026-11-08T07:00:00Z");

    private static final ExcelProperties PROPERTIES = new ExcelProperties(
            MAX_ROWS,
            new ExcelProperties.Column(40, 2),
            new ExcelProperties.Format("dd/mm/yyyy", "dd/mm/yyyy hh:mm"),
            ZoneId.of("Indian/Antananarivo"));

    private PdfExporter sut;

    @BeforeEach
    void setUp() {
        sut = new PdfExporter(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void export_rows_writesTitleHeadersAndFrenchFormattedValues() throws IOException {
        // Arrange
        var row = new SaleRow(
                "Savon Ravo", new BigDecimal("2500"), LocalDate.of(2026, 10, 19), true, Status.PAID, 1234567L);

        // Act
        List<String> pages = export(PdfExporter.report("Livre des ventes", SaleRow.class, Stream.of(row)));

        // Assert
        assertThat(pages).hasSize(1);
        assertThat(pages.getFirst())
                .contains("Livre des ventes")
                .contains("Article", "Montant", "Date", "Livrée", "Statut", "Quantité")
                .contains("Savon Ravo", "2 500,00", "19/10/2026", "Oui", "PAID", "1 234 567")
                .contains("Généré le 08/11/2026 10:00 · Page 1 / 1")
                .doesNotContain("note interne");
    }

    @Test
    void export_manyRows_repeatsHeaderOnEveryPageAndNumbersPages() throws IOException {
        // Arrange
        var data = IntStream.rangeClosed(1, MAX_ROWS).mapToObj(PdfExporterTest::sale);

        // Act
        List<String> pages = export(PdfExporter.report("Ventes", SaleRow.class, data));

        // Assert
        assertThat(pages).hasSizeGreaterThan(1);
        for (int i = 0; i < pages.size(); i++) {
            assertThat(pages.get(i)).contains("Montant").contains("Page " + (i + 1) + " / " + pages.size());
        }
        assertThat(String.join("", pages)).contains("Article 1", "Article " + MAX_ROWS);
    }

    @Test
    void export_emptyStream_writesHeaderAndEmptyMessage() throws IOException {
        // Act
        List<String> pages = export(PdfExporter.report("Ventes", SaleRow.class, Stream.empty()));

        // Assert
        assertThat(pages).hasSize(1);
        assertThat(pages.getFirst()).contains("Montant").contains("Aucune ligne à exporter.");
    }

    @Test
    void export_detailsAndSummary_areWrittenAroundTheTable() throws IOException {
        // Arrange
        var report = PdfExporter.report("Facture FAC-2026-0001", SaleRow.class, Stream.of(sale(1)))
                .withDetails(List.of("Client : Rasoa Hanta"))
                .withSummary(List.of("Total TTC : 25 000 Ar"));

        // Act
        String page = export(report).getFirst();

        // Assert
        assertThat(page.indexOf("Client : Rasoa Hanta")).isLessThan(page.indexOf("Article 1"));
        assertThat(page.indexOf("Total TTC : 25 000 Ar")).isGreaterThan(page.indexOf("Article 1"));
    }

    @Test
    void export_unprintableCharacters_areReplacedInsteadOfFailing() throws IOException {
        // Arrange
        var row = new SaleRow("Crème\tvisage → 🙂", BigDecimal.ONE, null, false, null, 0L);

        // Act
        var report = PdfExporter.report("Ventes", SaleRow.class, Stream.of(row));
        String page = export(report).getFirst();

        // Assert
        assertThat(page).contains("Crème visage ? ?").contains("Non");
    }

    @Test
    void export_longValue_isTruncatedWithEllipsis() throws IOException {
        // Arrange
        var row = new SaleRow("x".repeat(600), BigDecimal.ONE, null, false, null, 0L);

        // Act
        var report = PdfExporter.report("Ventes", SaleRow.class, Stream.of(row));
        String page = export(report).getFirst();

        // Assert
        assertThat(page).contains("x…").doesNotContain("x".repeat(600));
    }

    @Test
    void export_instant_isWrittenInConfiguredTimeZone() throws IOException {
        // Arrange
        var event = new EventRow(Instant.parse("2026-10-19T21:30:00Z"));

        // Act
        var report = PdfExporter.report("Journal", EventRow.class, Stream.of(event));
        String page = export(report).getFirst();

        // Assert
        assertThat(page).contains("20/10/2026 00:30");
    }

    @Test
    void export_exactlyMaxRows_writesEveryRow() throws IOException {
        // Arrange
        var data = Stream.generate(() -> sale(7)).limit(MAX_ROWS);

        // Act
        String text = String.join("", export(PdfExporter.report("Ventes", SaleRow.class, data)));

        // Assert
        assertThat(text.split("Article 7", -1)).hasSize(MAX_ROWS + 1);
    }

    @Test
    void export_moreThanMaxRows_failsWithoutWritingAnything() {
        // Arrange
        var produced = new AtomicInteger();
        var data = Stream.generate(() -> sale(produced.incrementAndGet()));
        var out = new ByteArrayOutputStream();

        // Act + Assert
        assertThatThrownBy(() -> sut.export(SaleRow.class, "Ventes", data, out))
                .isInstanceOf(ExcelRowLimitExceededException.class)
                .hasMessageContaining(String.valueOf(MAX_ROWS));
        assertThat(produced).hasValue(MAX_ROWS + 1);
        assertThat(out.size()).isZero();
    }

    private List<String> export(PdfExporter.Report<?> report) throws IOException {
        var out = new ByteArrayOutputStream();
        sut.export(report, out);
        var pages = new ArrayList<String>();
        try (PDDocument document = Loader.loadPDF(out.toByteArray())) {
            var stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }
        }
        return pages;
    }

    private static SaleRow sale(int number) {
        return new SaleRow("Article " + number, BigDecimal.TEN, LocalDate.of(2026, 1, 2), false, null, 1L);
    }

    enum Status {
        PAID
    }

    record SaleRow(
            @ExcelColumn(header = "Article", order = 1) String label,
            @ExcelColumn(header = "Montant", order = 2, format = "#,##0.00") BigDecimal amount,
            @ExcelColumn(header = "Date", order = 3) LocalDate date,
            @ExcelColumn(header = "Livrée", order = 4) boolean delivered,
            @ExcelColumn(header = "Statut", order = 5) Status status,
            @ExcelColumn(header = "Quantité", order = 6) long quantity,
            String note) {

        SaleRow(String label, BigDecimal amount, LocalDate date, boolean delivered, Status status, long quantity) {
            this(label, amount, date, delivered, status, quantity, "note interne");
        }
    }

    record EventRow(@ExcelColumn(header = "Quand") Instant at) {}
}
