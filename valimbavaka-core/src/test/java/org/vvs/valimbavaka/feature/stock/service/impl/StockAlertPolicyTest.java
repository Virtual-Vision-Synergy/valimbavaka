package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;

class StockAlertPolicyTest {

    @ParameterizedTest(name = "stock {0} / seuil {1} -> alerte {2}")
    @CsvSource({"0, 40, true", "39.999, 40, true", "40, 40, false", "41, 40, false", "0, 0, false", "5, 0, false"})
    void isUnderThreshold_bounds_alertOnlyStrictlyUnderAPositiveThreshold(
            String total, String threshold, boolean expected) {
        // Act
        var result = StockAlertPolicy.isUnderThreshold(stock("ART-0001", total, threshold));

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest(name = "manque {0} -> suggéré {1}")
    @CsvSource({"0.5, 10", "3, 10", "10, 10", "10.001, 20", "12.5, 20", "40, 40", "95, 100"})
    void suggestedQuantity_shortage_roundsUpToTheNextTenWithAMinimumOfTen(String shortage, String expected) {
        // Act
        var result = StockAlertPolicy.suggestedQuantity(new BigDecimal(shortage));

        // Assert
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void toAlert_stockUnderThreshold_returnsQuantityThresholdShortageAndSuggestion() {
        // Act
        var result = StockAlertPolicy.toAlert(stock("ART-0031", "12.5", "40"));

        // Assert
        assertThat(result.code()).isEqualTo("ART-0031");
        assertThat(result.quantity()).isEqualByComparingTo("12.5");
        assertThat(result.threshold()).isEqualByComparingTo("40");
        assertThat(result.shortage()).isEqualByComparingTo("27.5");
        assertThat(result.suggestedQuantity()).isEqualByComparingTo("30");
    }

    @Test
    void mostUrgentFirst_severalAlerts_sortsByCoverageThenByCode() {
        // Arrange
        var half = StockAlertPolicy.toAlert(stock("ART-0001", "20", "40"));
        var outB = StockAlertPolicy.toAlert(stock("ART-0003", "0", "10"));
        var outA = StockAlertPolicy.toAlert(stock("ART-0002", "0", "50"));
        var tenth = StockAlertPolicy.toAlert(stock("ART-0004", "10", "100"));

        // Act
        var result = List.of(half, outB, outA, tenth).stream()
                .sorted(StockAlertPolicy.MOST_URGENT_FIRST)
                .toList();

        // Assert
        assertThat(result).containsExactly(outA, outB, tenth, half);
    }

    private static StockRDto stock(String code, String total, String threshold) {
        return new StockRDto(
                UUID.randomUUID(),
                code,
                "Savon noir 100 g",
                null,
                ArticleUnit.PIECE,
                new BigDecimal(threshold),
                null,
                new BigDecimal(total),
                List.of());
    }
}
