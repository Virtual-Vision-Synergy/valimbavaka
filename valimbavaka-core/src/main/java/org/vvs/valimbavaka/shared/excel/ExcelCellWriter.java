package org.vvs.valimbavaka.shared.excel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;

/** Écrit les cellules d'un classeur ; un style par format pour tout le classeur (Excel limite leur nombre). */
final class ExcelCellWriter {

    private static final int BOOLEAN_LENGTH = "FALSE".length();

    private final Workbook workbook;
    private final ExcelProperties properties;
    private final CellStyle headerStyle;
    private final Map<String, CellStyle> stylesByFormat = new HashMap<>();

    ExcelCellWriter(Workbook workbook, ExcelProperties properties) {
        this.workbook = workbook;
        this.properties = properties;
        this.headerStyle = workbook.createCellStyle();
        Font bold = workbook.createFont();
        bold.setBold(true);
        headerStyle.setFont(bold);
    }

    void writeHeader(Cell cell, String header) {
        cell.setCellValue(header);
        cell.setCellStyle(headerStyle);
    }

    /** Écrit la valeur et retourne sa longueur affichée, qui sert à ajuster la largeur de la colonne. */
    int write(Cell cell, @Nullable Object value, String format) {
        if (value == null) {
            return 0;
        }
        String defaultFormat = defaultFormat(value);
        String effectiveFormat = format.isEmpty() ? defaultFormat : format;
        setValue(cell, value);
        if (!effectiveFormat.isEmpty()) {
            cell.setCellStyle(stylesByFormat.computeIfAbsent(effectiveFormat, this::createStyle));
        }
        return defaultFormat.isEmpty() ? displayLength(value) : effectiveFormat.length();
    }

    private void setValue(Cell cell, Object value) {
        switch (value) {
            case Number number -> cell.setCellValue(number.doubleValue());
            case Boolean bool -> cell.setCellValue(bool);
            case LocalDate date -> cell.setCellValue(date);
            case LocalDateTime dateTime -> cell.setCellValue(dateTime);
            case Instant instant -> cell.setCellValue(atZone(instant));
            case OffsetDateTime dateTime -> cell.setCellValue(atZone(dateTime.toInstant()));
            case ZonedDateTime dateTime -> cell.setCellValue(atZone(dateTime.toInstant()));
            case Date date -> cell.setCellValue(date);
            case Enum<?> constant -> cell.setCellValue(constant.name());
            default -> cell.setCellValue(value.toString());
        }
    }

    /** Format appliqué quand l'annotation n'en donne pas ; vide pour les valeurs qui ne sont pas des dates. */
    private String defaultFormat(Object value) {
        return switch (value) {
            case LocalDate date -> properties.format().date();
            case LocalDateTime dateTime -> properties.format().datetime();
            case Instant instant -> properties.format().datetime();
            case OffsetDateTime dateTime -> properties.format().datetime();
            case ZonedDateTime dateTime -> properties.format().datetime();
            case Date date -> properties.format().datetime();
            default -> "";
        };
    }

    private static int displayLength(Object value) {
        return switch (value) {
            case Boolean bool -> BOOLEAN_LENGTH;
            case BigDecimal decimal -> decimal.toPlainString().length();
            case Enum<?> constant -> constant.name().length();
            default -> value.toString().length();
        };
    }

    private LocalDateTime atZone(Instant instant) {
        return LocalDateTime.ofInstant(instant, properties.timeZone());
    }

    private CellStyle createStyle(String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(format));
        return style;
    }
}
