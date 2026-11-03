package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierLeadTimeRDto;

class StockAlertPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);
    private static final SupplierLeadTimeRDto FOREIGN_SUPPLIER =
            new SupplierLeadTimeRDto(UUID.randomUUID(), "Provence Essences", SupplierType.FOREIGN, 30);
    private static final SupplierLeadTimeRDto LOCAL_SUPPLIER =
            new SupplierLeadTimeRDto(UUID.randomUUID(), "Huiles Ravo", SupplierType.LOCAL, 7);

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
        var result = StockAlertPolicy.toAlert(stock("ART-0031", "12.5", "40"), null, TODAY);

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
        var half = StockAlertPolicy.toAlert(stock("ART-0001", "20", "40"), null, TODAY);
        var outB = StockAlertPolicy.toAlert(stock("ART-0003", "0", "10"), null, TODAY);
        var outA = StockAlertPolicy.toAlert(stock("ART-0002", "0", "50"), null, TODAY);
        var tenth = StockAlertPolicy.toAlert(stock("ART-0004", "10", "100"), null, TODAY);

        // Act
        var result = List.of(half, outB, outA, tenth).stream()
                .sorted(StockAlertPolicy.MOST_URGENT_FIRST)
                .toList();

        // Assert
        assertThat(result).containsExactly(outA, outB, tenth, half);
    }

    @Test
    void toAlert_foreignSupplier_returnsSupplierLeadTimeAndExpectedDeliveryDate() {
        // Act
        var result = StockAlertPolicy.toAlert(stock("ART-0031", "12.5", "40"), FOREIGN_SUPPLIER, TODAY);

        // Assert
        assertThat(result.supplierId()).isEqualTo(FOREIGN_SUPPLIER.id());
        assertThat(result.supplierName()).isEqualTo("Provence Essences");
        assertThat(result.leadTimeDays()).isEqualTo(30);
        assertThat(result.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 12, 3));
    }

    @Test
    void toAlert_zeroDayLeadTime_expectsDeliveryToday() {
        // Arrange
        var sameDay = new SupplierLeadTimeRDto(UUID.randomUUID(), "Atelier voisin", SupplierType.LOCAL, 0);

        // Act
        var result = StockAlertPolicy.toAlert(stock("ART-0031", "12.5", "40"), sameDay, TODAY);

        // Assert
        assertThat(result.leadTimeDays()).isZero();
        assertThat(result.expectedDeliveryDate()).isEqualTo(TODAY);
    }

    @Test
    void toAlert_noSupplier_leavesSupplierLeadTimeAndDateNull() {
        // Act
        var result = StockAlertPolicy.toAlert(stock("ART-0031", "12.5", "40"), null, TODAY);

        // Assert
        assertThat(result.supplierId()).isNull();
        assertThat(result.supplierName()).isNull();
        assertThat(result.leadTimeDays()).isNull();
        assertThat(result.expectedDeliveryDate()).isNull();
    }

    @Test
    void mostUrgentFirst_sameCoverage_putsTheLongestLeadTimeFirstThenNoSupplier() {
        // Arrange
        var local = StockAlertPolicy.toAlert(stock("ART-0001", "0", "40"), LOCAL_SUPPLIER, TODAY);
        var none = StockAlertPolicy.toAlert(stock("ART-0002", "0", "40"), null, TODAY);
        var foreign = StockAlertPolicy.toAlert(stock("ART-0003", "0", "40"), FOREIGN_SUPPLIER, TODAY);
        var halfForeign = StockAlertPolicy.toAlert(stock("ART-0004", "20", "40"), FOREIGN_SUPPLIER, TODAY);

        // Act
        var result = List.of(local, none, halfForeign, foreign).stream()
                .sorted(StockAlertPolicy.MOST_URGENT_FIRST)
                .toList();

        // Assert
        assertThat(result).containsExactly(foreign, local, none, halfForeign);
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
