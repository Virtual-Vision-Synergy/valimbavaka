package org.vvs.valimbavaka.feature.dashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PercentageSharesTest {

    @Test
    void of_threeEqualRevenues_sharesAddUpToExactlyOneHundred() {
        // Arrange
        var revenues = List.of(new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"));

        // Act
        var shares = PercentageShares.of(revenues);

        // Assert: 33.3 + 33.3 + 33.4, the leftover tenth goes to the first one on a tie
        assertThat(shares).containsExactly(new BigDecimal("33.4"), new BigDecimal("33.3"), new BigDecimal("33.3"));
        assertThat(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.0");
    }

    @Test
    void of_unevenRevenues_roundsToOneDecimalAndKeepsTheOrder() {
        // Arrange: 562 000 / 436 000 / 286 000 of 1 284 000 (rounded alone: 43.8 + 34.0 + 22.3 = 100.1)
        var revenues = List.of(new BigDecimal("562000"), new BigDecimal("436000"), new BigDecimal("286000"));

        // Act
        var shares = PercentageShares.of(revenues);

        // Assert
        assertThat(shares).containsExactly(new BigDecimal("43.8"), new BigDecimal("33.9"), new BigDecimal("22.3"));
    }

    @Test
    void of_oneRevenueOnly_getsAllTheShare() {
        var shares = PercentageShares.of(List.of(new BigDecimal("8500.00"), BigDecimal.ZERO));

        assertThat(shares).containsExactly(new BigDecimal("100.0"), new BigDecimal("0.0"));
    }

    @Test
    void of_zeroTotal_returnsZerosWithoutDividingByZero() {
        var shares = PercentageShares.of(List.of(BigDecimal.ZERO, new BigDecimal("0.00")));

        assertThat(shares).containsExactly(new BigDecimal("0.0"), new BigDecimal("0.0"));
    }

    @Test
    void of_noRevenue_returnsAnEmptyList() {
        assertThat(PercentageShares.of(List.of())).isEmpty();
    }

    @Test
    void of_tinyRevenueNextToABigOne_keepsTheSmallOneAtZeroPointZero() {
        var shares = PercentageShares.of(List.of(new BigDecimal("1000000"), new BigDecimal("1")));

        assertThat(shares).containsExactly(new BigDecimal("100.0"), new BigDecimal("0.0"));
    }
}
