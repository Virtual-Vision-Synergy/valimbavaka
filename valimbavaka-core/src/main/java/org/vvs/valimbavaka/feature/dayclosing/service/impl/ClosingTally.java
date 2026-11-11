package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import java.math.BigDecimal;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

/**
 * Quantities of one article in one location for the day. Exits are stored as positive quantities; a movement of a
 * type outside the six components (receipt, production, inventory) is left out and shows up in the gap.
 */
final class ClosingTally {

    private BigDecimal opening = BigDecimal.ZERO;
    private BigDecimal transferred = BigDecimal.ZERO;
    private BigDecimal returned = BigDecimal.ZERO;
    private BigDecimal sold = BigDecimal.ZERO;
    private BigDecimal delivered = BigDecimal.ZERO;
    private BigDecimal salon = BigDecimal.ZERO;
    private BigDecimal shooting = BigDecimal.ZERO;
    private BigDecimal systemStock = BigDecimal.ZERO;

    void setOpening(BigDecimal quantity) {
        this.opening = quantity;
    }

    void setSystemStock(BigDecimal quantity) {
        this.systemStock = quantity;
    }

    /**
     * Adds the day's movements of one type. A sale in the delivery location is a delivery: the order became a sale
     * when it was delivered (LIV-03).
     */
    void add(StockMovementType type, BigDecimal quantity, boolean deliveryLocation) {
        switch (type) {
            case TRANSFER -> transferred = transferred.add(quantity);
            case RETURN -> returned = returned.add(quantity);
            case SALE -> {
                if (deliveryLocation) {
                    delivered = delivered.subtract(quantity);
                } else {
                    sold = sold.subtract(quantity);
                }
            }
            case DELIVERY -> delivered = delivered.subtract(quantity);
            case TRADE_SHOW -> salon = salon.subtract(quantity);
            case SHOOTING -> shooting = shooting.subtract(quantity);
            case RECEIPT, PRODUCTION, INVENTORY -> {
                // outside the six components
            }
        }
    }

    BigDecimal opening() {
        return opening;
    }

    BigDecimal transferred() {
        return transferred;
    }

    BigDecimal returned() {
        return returned;
    }

    BigDecimal sold() {
        return sold;
    }

    BigDecimal delivered() {
        return delivered;
    }

    BigDecimal salon() {
        return salon;
    }

    BigDecimal shooting() {
        return shooting;
    }

    BigDecimal systemStock() {
        return systemStock;
    }

    /** Opening + transferred + returned - sold - delivered - salon - shooting. */
    BigDecimal theoretical() {
        return opening.add(transferred)
                .add(returned)
                .subtract(sold)
                .subtract(delivered)
                .subtract(salon)
                .subtract(shooting);
    }

    BigDecimal gap() {
        return systemStock.subtract(theoretical());
    }

    /** True when nothing was held, moved or counted: the article has no line. */
    boolean isEmpty() {
        return opening.signum() == 0
                && transferred.signum() == 0
                && returned.signum() == 0
                && sold.signum() == 0
                && delivered.signum() == 0
                && salon.signum() == 0
                && shooting.signum() == 0
                && systemStock.signum() == 0;
    }
}
