package org.vvs.valimbavaka.feature.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StockMovementTypeTest {

    @Test
    void accepts_receiptWithPositiveQuantity_returnsTrue() {
        assertThat(StockMovementType.RECEIPT.accepts(new BigDecimal("12"))).isTrue();
    }

    @Test
    void accepts_receiptWithNegativeQuantity_returnsFalse() {
        assertThat(StockMovementType.RECEIPT.accepts(new BigDecimal("-1"))).isFalse();
    }

    @Test
    void accepts_saleWithNegativeQuantity_returnsTrue() {
        assertThat(StockMovementType.SALE.accepts(new BigDecimal("-2"))).isTrue();
    }

    @Test
    void accepts_saleWithPositiveQuantity_returnsFalse() {
        assertThat(StockMovementType.SALE.accepts(new BigDecimal("2"))).isFalse();
    }

    @Test
    void accepts_inventoryWithEitherSign_returnsTrue() {
        assertThat(StockMovementType.INVENTORY.accepts(new BigDecimal("3"))).isTrue();
        assertThat(StockMovementType.INVENTORY.accepts(new BigDecimal("-3"))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(StockMovementType.class)
    void accepts_zeroQuantity_returnsFalseForEveryType(StockMovementType type) {
        assertThat(type.accepts(BigDecimal.ZERO)).isFalse();
    }
}
