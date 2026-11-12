package org.vvs.valimbavaka.feature.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.dto.ReportComparisonRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportInfoRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportPdfRowRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRangeRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRangesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRankedArticleRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRankingRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportTrendPointRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportVariationRDto;

class ReportExportLayoutTest {

    private static final ReportLabels ALL = new ReportLabels(null, null);

    private final ReportExportLayout sut = new ReportExportLayout();

    @Test
    void filename_eachGranularity_followsTheKindAndThePeriod() {
        assertThat(sut.filename(report(ReportGranularity.DAY, "2026-11-12", "2026-11-12"), "pdf"))
                .isEqualTo("rapport-journalier-2026-11-12.pdf");
        assertThat(sut.filename(report(ReportGranularity.MONTH, "2026-11-01", "2026-11-30"), "xlsx"))
                .isEqualTo("rapport-mensuel-2026-11.xlsx");
        assertThat(sut.filename(report(ReportGranularity.YEAR, "2026-01-01", "2026-12-31"), "xlsx"))
                .isEqualTo("rapport-annuel-2026.xlsx");
    }

    @Test
    void info_noFilter_saysAllChannelsAndAllRanges() {
        // Act
        var rows = sut.info(report(ReportGranularity.MONTH, "2026-11-01", "2026-11-30"), ALL);

        // Assert
        assertThat(rows)
                .extracting(ReportInfoRowRDto::label, ReportInfoRowRDto::value)
                .containsExactly(
                        tuple("Rapport", "Rapport mensuel"),
                        tuple("Période", "du 01/11/2026 au 30/11/2026"),
                        tuple("Période précédente", "du 01/10/2026 au 31/10/2026"),
                        tuple("Canal de vente", "Tous les canaux"),
                        tuple("Gamme", "Toutes les gammes"));
    }

    @Test
    void info_day_printsTheDateAloneAndTheFilterLabels() {
        // Act
        var rows = sut.info(
                report(ReportGranularity.DAY, "2026-11-12", "2026-11-12"), new ReportLabels("Boutique", "Soins"));

        // Assert
        assertThat(rows.get(1).value()).isEqualTo("12/11/2026");
        assertThat(rows.get(3).value()).isEqualTo("Boutique");
        assertThat(rows.get(4).value()).isEqualTo("Soins");
    }

    @Test
    void indicators_report_copiesTheFiguresOfTheScreen() {
        // Act
        var rows = sut.indicators(report(ReportGranularity.MONTH, "2026-11-01", "2026-11-30"));

        // Assert
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).current()).isEqualByComparingTo("12500.50");
        assertThat(rows.get(0).previous()).isEqualByComparingTo("10000.00");
        assertThat(rows.get(0).variation()).isEqualByComparingTo("25.0");
        assertThat(rows.get(1).current()).isEqualByComparingTo("5");
        assertThat(rows.get(1).variation()).isNull();
        assertThat(rows.get(2).indicator()).isEqualTo("Panier moyen (Ar)");
    }

    @Test
    void pdfDetails_report_printsFiltersAndIndicatorsInFrench() {
        // Act
        var lines = sut.pdfDetails(report(ReportGranularity.MONTH, "2026-11-01", "2026-11-30"), ALL);

        // Assert
        assertThat(lines)
                .contains("Chiffre d'affaires : 12 500,50 Ar (précédent : 10 000,00 Ar, +25,0 %)")
                .contains("Nombre de ventes : 5 (précédent : 4, n/d)")
                .contains("Panier moyen : 2 500,10 Ar (précédent : 2 500,00 Ar, -1,5 %)");
    }

    @Test
    void pdfRows_report_chainsTheCurveTheRangesAndTheRanking() {
        // Act
        var rows = sut.pdfRows(report(ReportGranularity.MONTH, "2026-11-01", "2026-11-30"));

        // Assert
        assertThat(rows)
                .extracting(ReportPdfRowRDto::section)
                .containsExactly("Évolution", "Évolution", "Gamme", "Classement");
        assertThat(rows.get(0).label()).isEqualTo("01/11/2026");
        assertThat(rows.get(2).label()).isEqualTo("Soins (SOINS)");
        assertThat(rows.get(2).share()).isEqualByComparingTo("100.0");
        assertThat(rows.get(3).label()).isEqualTo("Savon (A-01)");
        assertThat(rows.get(3).quantity()).isEqualByComparingTo("3");
        assertThat(rows.get(3).margin()).isNull();
    }

    @Test
    void pdfRows_year_labelsTheCurveByMonth() {
        // Act
        var rows = sut.pdfRows(report(ReportGranularity.YEAR, "2026-01-01", "2026-12-31"));

        // Assert
        assertThat(rows.get(0).label()).isEqualTo("11/2026");
    }

    private static ReportRDto report(ReportGranularity granularity, String from, String to) {
        var start = LocalDate.parse(from);
        var end = LocalDate.parse(to);
        var sales = new ReportSalesRDto(new BigDecimal("12500.50"), 5, new BigDecimal("2500.10"));
        var previous = new ReportSalesRDto(new BigDecimal("10000.00"), 4, new BigDecimal("2500.00"));
        var variation = new ReportVariationRDto(new BigDecimal("25.0"), null, new BigDecimal("-1.5"));
        var previousStart = previousFrom(granularity, start);
        var comparison = new ReportComparisonRDto(previousStart, start.minusDays(1), previous, variation);
        var trend = List.of(
                new ReportTrendPointRDto(LocalDate.parse("2026-11-01"), new BigDecimal("12500.50")),
                new ReportTrendPointRDto(LocalDate.parse("2026-11-02"), BigDecimal.ZERO));
        var range = new ReportRangeRDto(
                UUID.randomUUID(), "SOINS", "Soins", new BigDecimal("12500.50"), new BigDecimal("100.0"));
        var article = new ReportRankedArticleRDto(
                UUID.randomUUID(),
                "A-01",
                "Savon",
                new BigDecimal("3.000"),
                new BigDecimal("12500.50"),
                null,
                new BigDecimal("100.0"));
        return new ReportRDto(
                granularity,
                start,
                end,
                null,
                null,
                sales,
                comparison,
                trend,
                new ReportRangesRDto(new BigDecimal("12500.50"), List.of(range)),
                new ReportRankingRDto(10, new BigDecimal("12500.50"), List.of(article)));
    }

    private static LocalDate previousFrom(ReportGranularity granularity, LocalDate start) {
        return switch (granularity) {
            case DAY -> start.minusDays(1);
            case MONTH -> start.minusMonths(1);
            case YEAR -> start.minusYears(1);
        };
    }
}
