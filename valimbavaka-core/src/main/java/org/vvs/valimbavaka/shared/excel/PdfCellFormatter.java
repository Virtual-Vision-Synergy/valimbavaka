package org.vvs.valimbavaka.shared.excel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Texte d'une cellule PDF, à la française : espace des milliers, virgule décimale, dates {@code jj/mm/aaaa}.
 * Le {@code format} d'un {@code @ExcelColumn} numérique (ex. {@code "#,##0.00"}) est repris ; une instance par
 * export ({@link DecimalFormat} n'est pas thread-safe).
 */
final class PdfCellFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String INTEGER_PATTERN = "#,##0";
    private static final String DECIMAL_PATTERN = "#,##0.##";
    private static final String YES = "Oui";
    private static final String NO = "Non";

    private final ZoneId zone;
    private final Map<String, DecimalFormat> numberFormats = new HashMap<>();

    PdfCellFormatter(ZoneId zone) {
        this.zone = zone;
    }

    String format(@Nullable Object value, String format) {
        return switch (value) {
            case null -> "";
            case Number number -> formatNumber(number, format);
            case Boolean bool -> bool ? YES : NO;
            case Enum<?> constant -> constant.name();
            case Date date -> DATE_TIME.format(date.toInstant().atZone(zone));
            case Temporal temporal -> formatTemporal(temporal);
            default -> value.toString();
        };
    }

    private String formatTemporal(Temporal temporal) {
        return switch (temporal) {
            case LocalDate date -> DATE.format(date);
            case LocalDateTime dateTime -> DATE_TIME.format(dateTime);
            case Instant instant -> DATE_TIME.format(instant.atZone(zone));
            case OffsetDateTime dateTime -> DATE_TIME.format(dateTime.atZoneSameInstant(zone));
            case ZonedDateTime dateTime -> DATE_TIME.format(dateTime.withZoneSameInstant(zone));
            default -> temporal.toString();
        };
    }

    private String formatNumber(Number number, String format) {
        String pattern = format.isBlank() ? defaultPattern(number) : format;
        return numberFormats
                .computeIfAbsent(pattern, PdfCellFormatter::decimalFormat)
                .format(number);
    }

    private static String defaultPattern(Number number) {
        return number instanceof BigDecimal || number instanceof Double || number instanceof Float
                ? DECIMAL_PATTERN
                : INTEGER_PATTERN;
    }

    // Un format Excel que Java ne comprend pas (ex. date sur une colonne numérique) retombe sur le format décimal
    private static DecimalFormat decimalFormat(String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format;
        try {
            format = new DecimalFormat(pattern, symbols);
        } catch (IllegalArgumentException e) {
            format = new DecimalFormat(DECIMAL_PATTERN, symbols);
        }
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
