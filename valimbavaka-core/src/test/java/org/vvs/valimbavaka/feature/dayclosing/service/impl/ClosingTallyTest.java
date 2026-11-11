package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

class ClosingTallyTest {

    private final ClosingTally sut = new ClosingTally();

    @Test
    void theoretical_sixComponents_addsTheEntriesAndRemovesTheExits() {
        // Arrange
        sut.add(StockMovementType.TRANSFER, new BigDecimal("30"), false);
        sut.add(StockMovementType.RETURN, new BigDecimal("1"), false);
        sut.add(StockMovementType.SALE, new BigDecimal("-19"), false);
        sut.add(StockMovementType.DELIVERY, new BigDecimal("-2"), false);
        sut.add(StockMovementType.TRADE_SHOW, new BigDecimal("-3"), false);
        sut.add(StockMovementType.SHOOTING, new BigDecimal("-1"), false);

        // Act
        var theoretical = sut.theoretical();

        // Assert
        assertThat(sut.transferred()).isEqualByComparingTo("30");
        assertThat(sut.returned()).isEqualByComparingTo("1");
        assertThat(sut.sold()).isEqualByComparingTo("19");
        assertThat(sut.delivered()).isEqualByComparingTo("2");
        assertThat(sut.salon()).isEqualByComparingTo("3");
        assertThat(sut.shooting()).isEqualByComparingTo("1");
        assertThat(theoretical).isEqualByComparingTo("6");
    }

    @Test
    void theoretical_openingStock_isCarriedIntoTheTotal() {
        // Arrange
        sut.setOpening(new BigDecimal("4"));
        sut.add(StockMovementType.SALE, new BigDecimal("-1"), false);

        // Act + Assert
        assertThat(sut.theoretical()).isEqualByComparingTo("3");
    }

    @Test
    void add_saleInTheDeliveryLocation_countsAsDelivered() {
        // Act
        sut.add(StockMovementType.SALE, new BigDecimal("-6"), true);

        // Assert
        assertThat(sut.delivered()).isEqualByComparingTo("6");
        assertThat(sut.sold()).isZero();
    }

    @Test
    void add_transferLeavingTheLocation_lowersTheTransferredQuantity() {
        // Act
        sut.add(StockMovementType.TRANSFER, new BigDecimal("10"), false);
        sut.add(StockMovementType.TRANSFER, new BigDecimal("-4"), false);

        // Assert
        assertThat(sut.transferred()).isEqualByComparingTo("6");
    }

    @Test
    void gap_movementOutsideTheSixComponents_showsInTheGap() {
        // Arrange
        sut.add(StockMovementType.TRANSFER, new BigDecimal("10"), false);
        sut.add(StockMovementType.INVENTORY, new BigDecimal("-2"), false);
        sut.setSystemStock(new BigDecimal("8"));

        // Act + Assert
        assertThat(sut.theoretical()).isEqualByComparingTo("10");
        assertThat(sut.gap()).isEqualByComparingTo("-2");
    }

    @Test
    void gap_stockMatchingTheTheoretical_isZero() {
        // Arrange
        sut.add(StockMovementType.TRANSFER, new BigDecimal("10"), false);
        sut.add(StockMovementType.SALE, new BigDecimal("-3"), false);
        sut.setSystemStock(new BigDecimal("7"));

        // Act + Assert
        assertThat(sut.gap()).isZero();
    }

    @Test
    void isEmpty_nothingHeldNorMoved_isTrue() {
        assertThat(sut.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_anyQuantity_isFalse() {
        // Arrange
        sut.add(StockMovementType.SHOOTING, new BigDecimal("-1"), false);

        // Act + Assert
        assertThat(sut.isEmpty()).isFalse();
    }
}
