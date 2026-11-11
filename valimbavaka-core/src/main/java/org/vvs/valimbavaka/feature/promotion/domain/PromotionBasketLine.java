package org.vvs.valimbavaka.feature.promotion.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * One line of a basket to price (till, PRO-02): article, its range (null if none), catalogue unit price and quantity.
 */
public record PromotionBasketLine(UUID articleId, @Nullable UUID rangeId, BigDecimal unitPrice, BigDecimal quantity) {

    /** Same rule as a sale line (VTE-01): unit price times quantity, rounded half up to 2 decimals. */
    public BigDecimal total() {
        return unitPrice.setScale(2, RoundingMode.HALF_UP).multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }
}
