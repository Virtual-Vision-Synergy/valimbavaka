package org.vvs.valimbavaka.feature.promotion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** PRO-02: the discount of a promotion on a basket, with the rounding and the caps fixed by the PRO-01 guide. */
class PromotionPricingTest {

    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();
    private static final UUID RANGE = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 11);

    @Test
    void discountOf_percentOnTheWholeCatalogue_takesTheRateOfEachLine() {
        // Arrange : 2 × 8 500 = 17 000 and 1 × 12 000
        var sut = promotion(PromotionKind.PERCENT, "15", PromotionScope.ALL, Set.of());
        var basket = List.of(line(SOAP, null, "8500", "2"), line(OIL, null, "12000", "1"));

        // Act
        var discount = PromotionPricing.discountOf(sut, basket);

        // Assert : 2 550 + 1 800
        assertThat(discount).isEqualByComparingTo("4350");
    }

    @Test
    void discountOf_percentHalfAriary_roundsHalfUpToTheAriary() {
        // Arrange : 3 × 335 = 1 005 → 10 % = 100,5 Ar
        var sut = promotion(PromotionKind.PERCENT, "10", PromotionScope.ALL, Set.of());

        // Act
        var discount = PromotionPricing.discountOf(sut, List.of(line(SOAP, null, "335", "3")));

        // Assert
        assertThat(discount).isEqualByComparingTo("101");
    }

    @Test
    void discountOf_percentOnDecimalQuantity_usesTheLineTotalRoundedToTheCentime() {
        // Arrange : 1,5 × 3 333,33 = 4 999,995 → 5 000,00 ; 12,5 % = 625 Ar
        var sut = promotion(PromotionKind.PERCENT, "12.5", PromotionScope.ALL, Set.of());

        // Act
        var discount = PromotionPricing.discountOf(sut, List.of(line(SOAP, null, "3333.33", "1.5")));

        // Assert
        assertThat(discount).isEqualByComparingTo("625");
    }

    @Test
    void discountOf_percentHundred_neverExceedsTheLineTotal() {
        // Arrange
        var sut = promotion(PromotionKind.PERCENT, "100", PromotionScope.ALL, Set.of());

        // Act
        var discount = PromotionPricing.discountOf(sut, List.of(line(SOAP, null, "8500", "2")));

        // Assert
        assertThat(discount).isEqualByComparingTo("17000");
    }

    @ParameterizedTest
    @CsvSource({"5000, 5000", "17000, 17000", "20000, 17000", "0.5, 1"})
    void discountOf_amount_isTakenOnceOffTheEligibleTotalAndCappedAtIt(String value, String expected) {
        // Arrange : two eligible lines, 8 500 + 8 500
        var sut = promotion(PromotionKind.AMOUNT, value, PromotionScope.ALL, Set.of());
        var basket = List.of(line(SOAP, null, "8500", "1"), line(OIL, null, "8500", "1"));

        // Act
        var discount = PromotionPricing.discountOf(sut, basket);

        // Assert
        assertThat(discount).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"2, 0", "3, 8500", "5, 8500", "6, 17000", "7, 17000", "3.5, 8500", "2.999, 0"})
    void discountOf_buyThree_givesOneFreeUnitPerWholeGroupOfThree(String quantity, String expected) {
        // Arrange
        var sut = promotion(PromotionKind.BUY_X, "3", PromotionScope.ALL, Set.of());

        // Act
        var discount = PromotionPricing.discountOf(sut, List.of(line(SOAP, null, "8500", quantity)));

        // Assert
        assertThat(discount).isEqualByComparingTo(expected);
    }

    @Test
    void discountOf_rangesScope_onlyCountsTheLinesOfTheTargetedRange() {
        // Arrange
        var sut = promotion(PromotionKind.PERCENT, "10", PromotionScope.RANGES, Set.of(RANGE));
        var basket = List.of(line(SOAP, RANGE, "10000", "1"), line(OIL, UUID.randomUUID(), "10000", "1"));

        // Act
        var discount = PromotionPricing.discountOf(sut, basket);

        // Assert
        assertThat(discount).isEqualByComparingTo("1000");
    }

    @Test
    void discountOf_articlesScope_onlyCountsTheTargetedArticles() {
        // Arrange
        var sut = promotion(PromotionKind.AMOUNT, "3000", PromotionScope.ARTICLES, Set.of(OIL));
        var basket = List.of(line(SOAP, RANGE, "10000", "1"), line(OIL, null, "12000", "1"));

        // Act
        var discount = PromotionPricing.discountOf(sut, basket);

        // Assert
        assertThat(discount).isEqualByComparingTo("3000");
    }

    @Test
    void discountOf_noLineReached_isZero() {
        // Arrange : the article has no range, the promotion targets a range
        var sut = promotion(PromotionKind.PERCENT, "10", PromotionScope.RANGES, Set.of(RANGE));

        // Act
        var discount = PromotionPricing.discountOf(sut, List.of(line(SOAP, null, "10000", "1")));

        // Assert
        assertThat(discount).isEqualByComparingTo("0");
    }

    @Test
    void discountOf_emptyBasket_isZero() {
        // Arrange
        var sut = promotion(PromotionKind.AMOUNT, "3000", PromotionScope.ALL, Set.of());

        // Act + Assert
        assertThat(PromotionPricing.discountOf(sut, List.of())).isEqualByComparingTo("0");
    }

    private static PromotionBasketLine line(UUID articleId, UUID rangeId, String unitPrice, String quantity) {
        return new PromotionBasketLine(articleId, rangeId, new BigDecimal(unitPrice), new BigDecimal(quantity));
    }

    private static Promotion promotion(PromotionKind kind, String value, PromotionScope scope, Set<UUID> targets) {
        return Promotion.create(new PromotionDetails(
                "Octobre rose",
                null,
                kind,
                new BigDecimal(value),
                null,
                scope,
                targets,
                new PromotionPeriod(DAY, DAY.plusDays(30)),
                Set.of(UUID.randomUUID()),
                null));
    }
}
