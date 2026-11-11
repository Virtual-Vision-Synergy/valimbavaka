package org.vvs.valimbavaka.feature.sale.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

class SaleTest {

    private static final SaleHeader HEADER = new SaleHeader(
            UUID.randomUUID(),
            LocalDate.of(2026, 11, 8),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            PaymentMethod.CASH);

    @Test
    void record_twoLines_freezesThePricesAndAddsTheLineTotals() {
        // Arrange
        var soap = new PricedSaleLine(UUID.randomUUID(), new BigDecimal("3"), new BigDecimal("8500"), null);
        var cost = new BigDecimal("7000");
        var oil = new PricedSaleLine(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("12000"), cost);

        // Act
        var sut = Sale.record("VTE-0042", HEADER, List.of(soap, oil));

        // Assert
        assertThat(sut.getStatus()).isEqualTo(OperationStatus.VALIDATED);
        assertThat(sut.getLines())
                .extracting(SaleLine::getLineTotal)
                .containsExactly(new BigDecimal("25500.00"), new BigDecimal("12000.00"));
        assertThat(sut.getLines().get(1).getUnitCost()).isEqualByComparingTo("7000");
        assertThat(sut.getSubtotal()).isEqualByComparingTo("37500");
        assertThat(sut.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(sut.getTotal()).isEqualByComparingTo("37500");
        assertThat(sut.getStockLocationId()).isEqualTo(HEADER.stockLocationId());
        assertThat(sut.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void record_decimalQuantity_roundsTheLineTotalHalfUpToTwoDecimals() {
        // Arrange : 1,5 kg × 3 333,33 Ar = 4 999,995 Ar
        var line = new PricedSaleLine(UUID.randomUUID(), new BigDecimal("1.5"), new BigDecimal("3333.33"), null);

        // Act
        var sut = Sale.record("VTE-0043", HEADER, List.of(line));

        // Assert
        assertThat(sut.getLines().getFirst().getLineTotal()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(sut.getTotal()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void cancel_validatedSale_becomesCancelled() {
        // Arrange
        var sut = Sale.record("VTE-0044", HEADER, List.of(line()));

        // Act
        sut.cancel();

        // Assert
        assertThat(sut.getStatus()).isEqualTo(OperationStatus.CANCELLED);
    }

    @Test
    void cancel_alreadyCancelledSale_throwsConflict() {
        // Arrange
        var sut = Sale.record("VTE-0044", HEADER, List.of(line()));
        sut.cancel();

        // Act + Assert
        assertThatThrownBy(sut::cancel).isInstanceOf(ConflictException.class);
        assertThat(sut.getStatus()).isEqualTo(OperationStatus.CANCELLED);
    }

    private static PricedSaleLine line() {
        return new PricedSaleLine(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("8500"), null);
    }

    @Test
    void record_withDiscount_subtractsItFromTheSubtotalAndKeepsThePromotion() {
        // Arrange
        var promotionId = UUID.randomUUID();
        var line = new PricedSaleLine(UUID.randomUUID(), new BigDecimal("3"), new BigDecimal("8500"), null);

        // Act
        var sut = Sale.record("VTE-0045", HEADER, List.of(line), new SaleDiscount(promotionId, new BigDecimal("2550")));

        // Assert
        assertThat(sut.getSubtotal()).isEqualByComparingTo("25500");
        assertThat(sut.getDiscountAmount()).isEqualByComparingTo("2550.00");
        assertThat(sut.getTotal()).isEqualByComparingTo("22950");
        assertThat(sut.getPromotionId()).isEqualTo(promotionId);
    }

    @Test
    void record_discountAboveTheSubtotal_capsItSoTheTotalIsNeverNegative() {
        // Arrange
        var line = new PricedSaleLine(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("1000"), null);
        var discount = new SaleDiscount(UUID.randomUUID(), new BigDecimal("5000"));

        // Act
        var sut = Sale.record("VTE-0046", HEADER, List.of(line), discount);

        // Assert
        assertThat(sut.getDiscountAmount()).isEqualByComparingTo("1000");
        assertThat(sut.getTotal()).isEqualByComparingTo("0");
    }

    @Test
    void record_zeroDiscount_forgetsThePromotion() {
        // Arrange
        var line = new PricedSaleLine(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("1000"), null);

        // Act
        var sut = Sale.record("VTE-0047", HEADER, List.of(line), new SaleDiscount(UUID.randomUUID(), BigDecimal.ZERO));

        // Assert
        assertThat(sut.getPromotionId()).isNull();
        assertThat(sut.getTotal()).isEqualByComparingTo("1000");
    }
}
