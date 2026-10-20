package org.vvs.valimbavaka.shared.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;
import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;
import org.vvs.valimbavaka.shared.excel.exception.ExcelRowLimitExceededException;

class ExcelExporterTest {

    private static final int MAX_ROWS = 5;

    private static final ExcelProperties PROPERTIES = new ExcelProperties(
            MAX_ROWS,
            new ExcelProperties.Column(40, 2),
            new ExcelProperties.Format("dd/mm/yyyy", "dd/mm/yyyy hh:mm"),
            ZoneId.of("Indian/Antananarivo"));

    private ExcelExporter sut;

    @BeforeEach
    void setUp() {
        sut = new ExcelExporter(PROPERTIES);
    }

    @Test
    void export_classWithParent_sortsByOrderThenParentColumnsFirst() throws IOException {
        // Arrange
        var item = new ChildRow("Rabe", "Ivato", Status.ACTIVE);

        // Act
        XSSFSheet sheet = export(ChildRow.class, Stream.of(item));

        // Assert
        assertThat(texts(sheet.getRow(0))).containsExactly("Ville", "Nom", "Code", "Pays");
        assertThat(texts(sheet.getRow(1))).containsExactly("Ivato", "Rabe", "PARENT", "MG");
    }

    @Test
    void export_record_readsComponentsAndExpressions() throws IOException {
        // Arrange
        var item = new RecordRow(
                "Rasoa",
                new Category("Épicerie"),
                Status.ACTIVE,
                BigDecimal.valueOf(1234.5),
                LocalDate.of(2026, 10, 19),
                Instant.parse("2026-10-19T07:30:00Z"),
                true);

        // Act
        XSSFSheet sheet = export(RecordRow.class, Stream.of(item));

        // Assert
        assertThat(texts(sheet.getRow(1)))
                .containsExactly("RASOA", "Épicerie", "ACTIVE", "1,234.50", "19/10/2026", "19/10/2026 10:30", "TRUE");
    }

    @Test
    void export_nullInExpressionPath_leavesCellEmpty() throws IOException {
        // Arrange
        var item = new RecordRow("Rasoa", null, null, null, null, null, false);

        // Act
        XSSFSheet sheet = export(RecordRow.class, Stream.of(item));

        // Assert
        assertThat(texts(sheet.getRow(1))).containsExactly("RASOA", "", "", "", "", "", "FALSE");
    }

    @Test
    void export_unknownProperty_failsInsteadOfWritingEmptyCells() {
        // Arrange
        var data = Stream.of(new BrokenRow("valeur"));

        // Act + Assert
        assertThatThrownBy(() -> export(BrokenRow.class, data)).isInstanceOf(SpelEvaluationException.class);
    }

    @Test
    void export_longValue_widensColumnUpToConfiguredMaximum() throws IOException {
        // Arrange
        var data = Stream.of(new ChildRow("x".repeat(30), "y".repeat(100), Status.ACTIVE));

        // Act
        XSSFSheet sheet = export(ChildRow.class, data);

        // Assert
        assertThat(sheet.getColumnWidth(0)).isEqualTo(40 * 256);
        assertThat(sheet.getColumnWidth(1)).isEqualTo(32 * 256);
        assertThat(sheet.getColumnWidth(2)).isEqualTo(10 * 256);
    }

    @Test
    void export_exactlyMaxRows_writesEveryRow() throws IOException {
        // Arrange
        var data = Stream.generate(() -> new ChildRow("n", "v", Status.ACTIVE)).limit(MAX_ROWS);

        // Act
        XSSFSheet sheet = export(ChildRow.class, data);

        // Assert
        assertThat(sheet.getLastRowNum()).isEqualTo(MAX_ROWS);
    }

    @Test
    void export_moreThanMaxRows_failsWithoutConsumingTheRestOfTheStream() {
        // Arrange
        var produced = new AtomicInteger();
        var data = Stream.generate(() -> new ChildRow("n" + produced.incrementAndGet(), "v", Status.ACTIVE));
        var out = new ByteArrayOutputStream();

        // Act + Assert
        assertThatThrownBy(() -> sut.export(ChildRow.class, "Feuille", data, out))
                .isInstanceOf(ExcelRowLimitExceededException.class)
                .hasMessageContaining(String.valueOf(MAX_ROWS));
        assertThat(produced).hasValue(MAX_ROWS + 1);
        assertThat(out.size()).isZero();
    }

    @Test
    void export_severalSheets_writesOneTabPerSheetInOrder() throws IOException {
        // Arrange
        var out = new ByteArrayOutputStream();

        // Act
        sut.export(
                out,
                ExcelExporter.sheet(ChildRow.class, "Enfants", Stream.of(new ChildRow("a", "b", Status.ACTIVE))),
                ExcelExporter.sheet(BrokenRow.class, "Nom d'onglet trop long pour Excel [x]", Stream.empty()));

        // Assert
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Enfants");
            assertThat(workbook.getSheetName(1)).hasSizeLessThanOrEqualTo(31).doesNotContain("[");
        }
    }

    private <T> XSSFSheet export(Class<T> type, Stream<T> data) throws IOException {
        var out = new ByteArrayOutputStream();
        sut.export(type, "Feuille", data, out);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            return workbook.getSheetAt(0);
        }
    }

    private static List<String> texts(Row row) {
        var formatter = new DataFormatter(Locale.US);
        var values = new ArrayList<String>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(formatter.formatCellValue(row.getCell(i)));
        }
        return values;
    }

    enum Status {
        ACTIVE
    }

    record Category(String name) {}

    record RecordRow(
            @ExcelColumn(header = "Nom", order = 1, expression = "name.toUpperCase()")
            String name,

            @ExcelColumn(header = "Catégorie", order = 2, expression = "category?.name")
            Category category,

            @ExcelColumn(header = "Statut", order = 3) Status status,

            @ExcelColumn(header = "Solde", order = 4, format = "#,##0.00", width = 12)
            BigDecimal balance,

            @ExcelColumn(header = "Jour", order = 5) LocalDate day,
            @ExcelColumn(header = "Créé le", order = 6) Instant createdAt,
            @ExcelColumn(header = "Actif", order = 7) boolean active) {}

    record BrokenRow(
            @ExcelColumn(header = "Erreur", expression = "missing")
            String value) {}

    static class ParentRow {

        @ExcelColumn(header = "Code", width = 10)
        private final String code = "PARENT";

        public String getCode() {
            return code;
        }
    }

    static class ChildRow extends ParentRow {

        @ExcelColumn(header = "Nom", order = 2)
        private final String name;

        @ExcelColumn(header = "Ville", order = 1)
        private final String city;

        @ExcelColumn(header = "Pays")
        private final String country = "MG";

        private final Status ignored;

        ChildRow(String name, String city, Status ignored) {
            this.name = name;
            this.city = city;
            this.ignored = ignored;
        }

        public String getName() {
            return name;
        }

        public String getCity() {
            return city;
        }

        public String getCountry() {
            return country;
        }

        public Status getIgnored() {
            return ignored;
        }
    }
}
