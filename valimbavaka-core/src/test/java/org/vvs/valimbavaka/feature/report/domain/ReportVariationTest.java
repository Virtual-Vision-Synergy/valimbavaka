package org.vvs.valimbavaka.feature.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ReportVariationTest {

    @Test
    void percent_rise_isPositive() {
        // Act
        var result = ReportVariation.percent(new BigDecimal("24600000.00"), new BigDecimal("21540000.00"));

        // Assert
        assertThat(result).isEqualByComparingTo("14.2");
    }

    @Test
    void percent_fall_isNegative() {
        // Act
        var result = ReportVariation.percent(new BigDecimal("750.00"), new BigDecimal("1000.00"));

        // Assert
        assertThat(result).isEqualByComparingTo("-25.0");
    }

    @Test
    void percent_sameValue_isZero() {
        assertThat(ReportVariation.percent(5, 5)).isEqualByComparingTo("0.0");
    }

    @Test
    void percent_previousZero_isNull() {
        assertThat(ReportVariation.percent(BigDecimal.TEN, BigDecimal.ZERO)).isNull();
        assertThat(ReportVariation.percent(3, 0)).isNull();
        assertThat(ReportVariation.percent(0, 0)).isNull();
    }

    @Test
    void percent_halfWay_roundsHalfUpToOneDecimal() {
        // 1 / 8 = 12.5 % exactly; 1 / 16 = 6.25 % -> 6.3
        assertThat(ReportVariation.percent(9, 8)).isEqualByComparingTo("12.5");
        assertThat(ReportVariation.percent(17, 16)).isEqualByComparingTo("6.3");
    }

    @Test
    void percent_countsDoublingAndCollapse() {
        assertThat(ReportVariation.percent(8, 4)).isEqualByComparingTo("100.0");
        assertThat(ReportVariation.percent(0, 4)).isEqualByComparingTo("-100.0");
    }
}
