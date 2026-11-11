package org.vvs.valimbavaka.feature.commission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** COM-02: amount = base × rate / 100, rounded half up to the ariary; margin base never below zero. */
class CommissionTest {

    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID RULE_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 8);

    @Test
    void calculate_revenueRule_appliesTheRateToTheRevenue() {
        // Arrange : 25 500 Ar × 4 % = 1 020 Ar
        var terms = terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);

        // Act
        var sut = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);

        // Assert
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("1020.00"));
        assertThat(sut.getBaseAmount()).isEqualTo(new BigDecimal("25500.00"));
        assertThat(sut.getBase()).isEqualTo(CommissionBase.REVENUE);
        assertThat(sut.getRate()).isEqualTo(new BigDecimal("4.00"));
        assertThat(sut.getRateSource()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        assertThat(sut.getRuleId()).isEqualTo(RULE_ID);
        assertThat(sut.getSourceType()).isEqualTo(CommissionSourceType.SALE);
        assertThat(sut.getSourceId()).isEqualTo(SALE_ID);
        assertThat(sut.getSourceNumber()).isEqualTo("VTE-0042");
        assertThat(sut.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(sut.getSellerName()).isEqualTo("Miora Rakoto");
        assertThat(sut.getBusinessDate()).isEqualTo(DAY);
    }

    @Test
    void cancel_calculatedCommission_becomesCancelledAndKeepsItsAmount() {
        // Arrange
        var terms = terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);
        var sut = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);
        assertThat(sut.getStatus()).isEqualTo(CommissionStatus.CALCULATED);

        // Act
        sut.cancel();

        // Assert
        assertThat(sut.getStatus()).isEqualTo(CommissionStatus.CANCELLED);
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("1020.00"));
    }

    @Test
    void validate_calculatedCommission_becomesValidatedWithItsDate() {
        // Arrange
        var terms = terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);
        var sut = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);
        var at = Instant.parse("2026-11-11T08:00:00Z");

        // Act
        sut.validate(at);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(CommissionStatus.VALIDATED);
        assertThat(sut.getValidatedAt()).isEqualTo(at);
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("1020.00"));
    }

    @Test
    void validate_alreadyValidatedOrCancelled_isRefused() {
        // Arrange
        var terms = terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);
        var validated = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);
        validated.validate(Instant.parse("2026-11-11T08:00:00Z"));
        var cancelled = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);
        cancelled.cancel();

        // Act + Assert
        assertThatThrownBy(() -> validated.validate(Instant.now())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cancelled.validate(Instant.now())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancel_validatedCommission_becomesCancelledAndKeepsItsValidationDate() {
        // Arrange
        var terms = terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);
        var sut = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);
        var at = Instant.parse("2026-11-11T08:00:00Z");
        sut.validate(at);

        // Act
        sut.cancel();

        // Assert
        assertThat(sut.getStatus()).isEqualTo(CommissionStatus.CANCELLED);
        assertThat(sut.getValidatedAt()).isEqualTo(at);
    }

    @Test
    void calculate_fractionOfAriary_isRoundedHalfUpToTheAriary() {
        // Arrange : 25 500 Ar × 3,33 % = 849,15 Ar → 849 Ar ; 50 Ar × 1 % = 0,50 Ar → 1 Ar
        var rate = terms("3.33", CommissionBase.REVENUE, CommissionRateSource.SELLER, null);
        var half = terms("1", CommissionBase.REVENUE, CommissionRateSource.SELLER, null);

        // Act
        var down = Commission.calculate(sale("25500.00", null), "Miora", rate);
        var up = Commission.calculate(sale("50.00", null), "Miora", half);

        // Assert
        assertThat(down.getAmount()).isEqualTo(new BigDecimal("849.00"));
        assertThat(up.getAmount()).isEqualTo(new BigDecimal("1.00"));
    }

    @Test
    void calculate_marginRuleWithCost_appliesTheRateToRevenueMinusCost() {
        // Arrange : (17 000 − 6 241) Ar × 10 % = 1 075,90 Ar → 1 076 Ar
        var terms = terms("10", CommissionBase.MARGIN, CommissionRateSource.SALES_CHANNEL, RULE_ID);

        // Act
        var sut = Commission.calculate(sale("17000.00", "6241.00"), "Miora", terms);

        // Assert
        assertThat(sut.getBase()).isEqualTo(CommissionBase.MARGIN);
        assertThat(sut.getBaseAmount()).isEqualTo(new BigDecimal("10759.00"));
        assertThat(sut.getRevenue()).isEqualTo(new BigDecimal("17000.00"));
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("1076.00"));
    }

    @Test
    void calculate_marginRuleSoldBelowCost_baseAndAmountAreZero() {
        // Arrange
        var terms = terms("10", CommissionBase.MARGIN, CommissionRateSource.SALES_CHANNEL, RULE_ID);

        // Act
        var sut = Commission.calculate(sale("5000.00", "6000.00"), "Miora", terms);

        // Assert
        assertThat(sut.getBaseAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void calculate_marginRuleWithoutKnownCost_appliesTheRateToTheRevenue() {
        // Arrange : prestation salon, aucun coût figé
        var terms = terms("10", CommissionBase.MARGIN, CommissionRateSource.SALES_CHANNEL, RULE_ID);

        // Act
        var sut = Commission.calculate(sale("35000.00", null), "Miora", terms);

        // Assert
        assertThat(sut.getBase()).isEqualTo(CommissionBase.REVENUE);
        assertThat(sut.getBaseAmount()).isEqualTo(new BigDecimal("35000.00"));
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("3500.00"));
    }

    @Test
    void calculate_noRateFound_amountIsZero() {
        // Arrange
        var terms = terms("0.00", CommissionBase.REVENUE, CommissionRateSource.NONE, null);

        // Act
        var sut = Commission.calculate(sale("25500.00", null), "Miora", terms);

        // Assert
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(sut.getRateSource()).isEqualTo(CommissionRateSource.NONE);
    }

    @Test
    void recalculate_newRate_appliesItToTheSameRevenueAndReportsTheChange() {
        // Arrange : 25 500 Ar à 4 % = 1 020 Ar, puis le taux de la vendeuse passe à 5 %
        var sut = Commission.calculate(
                sale("25500.00", null),
                "Miora Rakoto",
                terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID));

        // Act
        var changed = sut.recalculate(null, terms("5", CommissionBase.REVENUE, CommissionRateSource.SELLER, null));

        // Assert : 25 500 × 5 % = 1 275 Ar, l'origine du taux suit, la règle de canal n'est plus citée
        assertThat(changed).isTrue();
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("1275.00"));
        assertThat(sut.getRate()).isEqualTo(new BigDecimal("5.00"));
        assertThat(sut.getRateSource()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(sut.getRuleId()).isNull();
        assertThat(sut.getRevenue()).isEqualTo(new BigDecimal("25500.00"));
        assertThat(sut.getStatus()).isEqualTo(CommissionStatus.CALCULATED);
    }

    @Test
    void recalculate_sameTerms_changesNothing() {
        // Arrange
        var terms = terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);
        var sut = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);

        // Act
        var changed = sut.recalculate(null, terms);

        // Assert
        assertThat(changed).isFalse();
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("1020.00"));
    }

    @Test
    void recalculate_marginRule_usesThePurchaseCostGivenAgain() {
        // Arrange : revenu 25 500, coût 20 000 : marge 5 500 × 10 % = 550 Ar
        var margin = terms("10", CommissionBase.MARGIN, CommissionRateSource.SALES_CHANNEL, RULE_ID);
        var sut = Commission.calculate(sale("25500.00", "20000.00"), "Miora Rakoto", margin);

        // Act : le coût d'achat réel est relu à 22 000
        var changed = sut.recalculate(new BigDecimal("22000.00"), margin);

        // Assert : 3 500 × 10 % = 350 Ar
        assertThat(changed).isTrue();
        assertThat(sut.getBaseAmount()).isEqualTo(new BigDecimal("3500.00"));
        assertThat(sut.getAmount()).isEqualTo(new BigDecimal("350.00"));
    }

    @Test
    void recalculate_validatedOrCancelledCommission_throwsAndKeepsItsAmount() {
        // Arrange
        var terms = terms("4", CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);
        var other = terms("9", CommissionBase.REVENUE, CommissionRateSource.SELLER, null);
        var validated = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);
        validated.validate(Instant.parse("2026-11-11T08:00:00Z"));
        var cancelled = Commission.calculate(sale("25500.00", null), "Miora Rakoto", terms);
        cancelled.cancel();

        // Act + Assert
        assertThatThrownBy(() -> validated.recalculate(null, other)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cancelled.recalculate(null, other)).isInstanceOf(IllegalStateException.class);
        assertThat(validated.getAmount()).isEqualTo(new BigDecimal("1020.00"));
        assertThat(cancelled.getAmount()).isEqualTo(new BigDecimal("1020.00"));
    }

    private static CommissionTerms terms(
            String rate, CommissionBase base, CommissionRateSource source, @Nullable UUID ruleId) {
        return new CommissionTerms(new BigDecimal(rate), base, source, ruleId);
    }

    private static CommissionSource sale(String revenue, @Nullable String purchaseCost) {
        return new CommissionSource(
                CommissionSourceType.SALE,
                SALE_ID,
                "VTE-0042",
                SELLER_ID,
                CHANNEL_ID,
                null,
                DAY,
                new BigDecimal(revenue),
                purchaseCost == null ? null : new BigDecimal(purchaseCost));
    }
}
