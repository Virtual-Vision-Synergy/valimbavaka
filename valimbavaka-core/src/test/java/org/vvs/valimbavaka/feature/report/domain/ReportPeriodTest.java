package org.vvs.valimbavaka.feature.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ReportPeriodTest {

    @Test
    void containing_day_isThatSingleDay() {
        // Act
        var sut = ReportPeriod.containing(ReportGranularity.DAY, LocalDate.of(2026, 11, 11));

        // Assert
        assertThat(sut.from()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(sut.to()).isEqualTo(LocalDate.of(2026, 11, 11));
    }

    @Test
    void containing_month_spansTheCalendarMonth() {
        // Act
        var sut = ReportPeriod.containing(ReportGranularity.MONTH, LocalDate.of(2026, 11, 11));

        // Assert
        assertThat(sut.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(sut.to()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void containing_februaryOfALeapYear_endsOnThe29th() {
        // Act
        var sut = ReportPeriod.containing(ReportGranularity.MONTH, LocalDate.of(2028, 2, 10));

        // Assert
        assertThat(sut.to()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void containing_year_spansTheCalendarYear() {
        // Act
        var sut = ReportPeriod.containing(ReportGranularity.YEAR, LocalDate.of(2026, 11, 11));

        // Assert
        assertThat(sut.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(sut.to()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void previous_dayOnNewYearsDay_isDecember31() {
        // Arrange
        var sut = ReportPeriod.containing(ReportGranularity.DAY, LocalDate.of(2026, 1, 1));

        // Act
        var result = sut.previous();

        // Assert
        assertThat(result.from()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(result.to()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void previous_month_isThePreviousCalendarMonth() {
        // Arrange
        var sut = ReportPeriod.containing(ReportGranularity.MONTH, LocalDate.of(2026, 3, 15));

        // Act
        var result = sut.previous();

        // Assert
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void previous_year_isThePreviousCalendarYear() {
        // Arrange
        var sut = ReportPeriod.containing(ReportGranularity.YEAR, LocalDate.of(2026, 6, 1));

        // Act
        var result = sut.previous();

        // Assert
        assertThat(result.from()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(result.to()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void startsAfter_dayInsideThePeriod_isFalse() {
        // Arrange
        var sut = ReportPeriod.containing(ReportGranularity.MONTH, LocalDate.of(2026, 11, 20));

        // Act + Assert
        assertThat(sut.startsAfter(LocalDate.of(2026, 11, 1))).isFalse();
        assertThat(sut.startsAfter(LocalDate.of(2026, 10, 31))).isTrue();
    }
}
