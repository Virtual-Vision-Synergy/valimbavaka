package org.vvs.valimbavaka.feature.sale.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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
}
