package org.vvs.valimbavaka.feature.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.dto.ReportIndicatorRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportInfoRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportPdfRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;

/**
 * Texts and rows around the figures of a report export (RPT-06), in French: dates {@code jj/mm/aaaa}, amounts in Ar,
 * variations with their sign. The figures themselves are copied from the {@link ReportRDto} of the screen.
 */
@Component
class ReportExportLayout {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final Map<ReportGranularity, String> KINDS = Map.of(
            ReportGranularity.DAY, "journalier",
            ReportGranularity.MONTH, "mensuel",
            ReportGranularity.YEAR, "annuel");
    private static final Map<ReportGranularity, String> FILE_DATES = Map.of(
            ReportGranularity.DAY, "yyyy-MM-dd",
            ReportGranularity.MONTH, "yyyy-MM",
            ReportGranularity.YEAR, "yyyy");
    private static final String NOT_AVAILABLE = "n/d";

    String title(ReportRDto report) {
        return "Rapport " + KINDS.get(report.granularity());
    }

    String filename(ReportRDto report, String extension) {
        var date = report.from().format(DateTimeFormatter.ofPattern(FILE_DATES.get(report.granularity())));
        return "rapport-%s-%s.%s".formatted(KINDS.get(report.granularity()), date, extension);
    }

    List<ReportInfoRowRDto> info(ReportRDto report, ReportLabels labels) {
        var comparison = report.comparison();
        return List.of(
                new ReportInfoRowRDto("Rapport", title(report)),
                new ReportInfoRowRDto("Période", period(report.from(), report.to())),
                new ReportInfoRowRDto("Période précédente", period(comparison.from(), comparison.to())),
                new ReportInfoRowRDto("Canal de vente", orDefault(labels.salesChannel(), "Tous les canaux")),
                new ReportInfoRowRDto("Gamme", orDefault(labels.range(), "Toutes les gammes")));
    }

    List<ReportIndicatorRowRDto> indicators(ReportRDto report) {
        var sales = report.sales();
        var previous = report.comparison().sales();
        var variation = report.comparison().variation();
        return List.of(
                new ReportIndicatorRowRDto(
                        "Chiffre d'affaires (Ar)", sales.revenue(), previous.revenue(), variation.revenue()),
                new ReportIndicatorRowRDto(
                        "Nombre de ventes",
                        BigDecimal.valueOf(sales.salesCount()),
                        BigDecimal.valueOf(previous.salesCount()),
                        variation.salesCount()),
                new ReportIndicatorRowRDto(
                        "Panier moyen (Ar)",
                        sales.averageBasket(),
                        previous.averageBasket(),
                        variation.averageBasket()));
    }

    /** Lines printed under the title of the PDF: the filters, then the indicators with their comparison. */
    List<String> pdfDetails(ReportRDto report, ReportLabels labels) {
        var lines = new ArrayList<String>();
        for (var row : info(report, labels)) {
            lines.add(row.label() + " : " + row.value());
        }
        var sales = report.sales();
        var before = report.comparison().sales();
        var variation = report.comparison().variation();
        lines.add(amountLine("Chiffre d'affaires", sales.revenue(), before.revenue(), variation.revenue()));
        lines.add(countLine("Nombre de ventes", sales.salesCount(), before.salesCount(), variation.salesCount()));
        lines.add(amountLine("Panier moyen", sales.averageBasket(), before.averageBasket(), variation.averageBasket()));
        return lines;
    }

    /** The turnover curve, the ranges then the ranking, one table for the PDF. */
    List<ReportPdfRowRDto> pdfRows(ReportRDto report) {
        var rows = new ArrayList<ReportPdfRowRDto>();
        var yearly = report.granularity() == ReportGranularity.YEAR;
        for (var point : report.trend()) {
            var label = yearly ? MONTH.format(point.date()) : DATE.format(point.date());
            rows.add(new ReportPdfRowRDto("Évolution", label, null, point.revenue(), null, null));
        }
        for (var range : report.ranges().items()) {
            var label = range.label() + " (" + range.code() + ")";
            rows.add(new ReportPdfRowRDto("Gamme", label, null, range.revenue(), null, range.share()));
        }
        for (var article : report.ranking().items()) {
            var label = article.name() + " (" + article.code() + ")";
            rows.add(new ReportPdfRowRDto(
                    "Classement", label, article.quantity(), article.revenue(), article.margin(), article.share()));
        }
        return rows;
    }

    private static String period(LocalDate from, LocalDate to) {
        return from.equals(to) ? DATE.format(from) : "du %s au %s".formatted(DATE.format(from), DATE.format(to));
    }

    private static String orDefault(@Nullable String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String amountLine(
            String label, BigDecimal current, BigDecimal previous, @Nullable BigDecimal variation) {
        return "%s : %s (précédent : %s, %s)".formatted(label, amount(current), amount(previous), percent(variation));
    }

    private static String countLine(String label, long current, long previous, @Nullable BigDecimal variation) {
        return "%s : %d (précédent : %d, %s)".formatted(label, current, previous, percent(variation));
    }

    private static String percent(@Nullable BigDecimal variation) {
        if (variation == null) {
            return NOT_AVAILABLE;
        }
        var sign = variation.signum() > 0 ? "+" : "";
        return sign + format(variation, "0.0") + " %";
    }

    private static String amount(BigDecimal value) {
        return format(value, "#,##0.00") + " Ar";
    }

    // One instance per call: DecimalFormat is not thread-safe
    private static String format(BigDecimal value, String pattern) {
        var symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        var format = new DecimalFormat(pattern, symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }
}
