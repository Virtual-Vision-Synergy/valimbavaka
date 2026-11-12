package org.vvs.valimbavaka.feature.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReportShareTest {

    @Test
    void distribute_twoAmounts_roundsToOneDecimal() {
        // Act
        var result = ReportShare.distribute(amounts(2, 1));

        // Assert
        assertThat(result).containsExactly(new BigDecimal("66.7"), new BigDecimal("33.3"));
    }

    @Test
    void distribute_threeEqualAmounts_giveTheMissingTenthToTheFirst() {
        // Act
        var result = ReportShare.distribute(amounts(10, 10, 10));

        // Assert: 33.3 + 33.3 + 33.3 = 99.9, the missing 0.1 goes to the first of the tie
        assertThat(result).containsExactly(new BigDecimal("33.4"), new BigDecimal("33.3"), new BigDecimal("33.3"));
    }

    @Test
    void distribute_missingTenthsGoToTheLargestRemainders() {
        // Act: exact shares 14.29 %, 28.57 %, 57.14 % -> rounded down 14.2, 28.5, 57.1 (99.8)
        var result = ReportShare.distribute(amounts(1, 2, 4));

        // Assert: the two missing tenths go to the first and the second, who lost the most
        assertThat(result).containsExactly(new BigDecimal("14.3"), new BigDecimal("28.6"), new BigDecimal("57.1"));
        assertThat(total(result)).isEqualByComparingTo("100.0");
    }

    @Test
    void distribute_singleAmount_is100() {
        // Act
        var result = ReportShare.distribute(List.of(new BigDecimal("12345.67")));

        // Assert
        assertThat(result).containsExactly(new BigDecimal("100.0"));
    }

    @Test
    void distribute_tinyAmountNextToHugeOne_isZeroPointZeroAndTheTotalStays100() {
        // Act
        var result = ReportShare.distribute(amounts(1_000_000, 1));

        // Assert
        assertThat(total(result)).isEqualByComparingTo("100.0");
        assertThat(result.get(1)).isEqualByComparingTo("0.0");
    }

    @Test
    void distribute_zeroTotalOrEmpty_givesZerosWithoutDividingByZero() {
        assertThat(ReportShare.distribute(amounts(0, 0))).containsExactly(new BigDecimal("0.0"), new BigDecimal("0.0"));
        assertThat(ReportShare.distribute(List.of())).isEmpty();
    }

    @Test
    void distribute_manyAmounts_alwaysAddUpTo100() {
        // Act
        var result = ReportShare.distribute(amounts(1, 2, 3, 7, 11, 13, 17));

        // Assert
        assertThat(total(result)).isEqualByComparingTo("100.0");
        assertThat(result).allSatisfy(share -> assertThat(share.scale()).isEqualTo(1));
    }

    private static List<BigDecimal> amounts(long... values) {
        return Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    private static BigDecimal total(List<BigDecimal> shares) {
        return shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
