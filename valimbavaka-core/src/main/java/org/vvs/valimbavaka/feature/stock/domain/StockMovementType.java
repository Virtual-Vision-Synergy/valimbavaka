package org.vvs.valimbavaka.feature.stock.domain;

import java.math.BigDecimal;

/** Types of stock movement (STK-01). The stock of an article is the sum of its movements, never edited by hand. */
public enum StockMovementType {
    RECEIPT(Sign.IN),
    TRANSFER(Sign.ANY),
    SALE(Sign.OUT),
    DELIVERY(Sign.OUT),
    RETURN(Sign.IN),
    TRADE_SHOW(Sign.ANY),
    SHOOTING(Sign.OUT),
    PRODUCTION(Sign.ANY),
    INVENTORY(Sign.ANY);

    private final Sign sign;

    StockMovementType(Sign sign) {
        this.sign = sign;
    }

    /** Quantity is signed: positive adds to the location, negative takes from it. */
    public boolean accepts(BigDecimal quantity) {
        return switch (sign) {
            case IN -> quantity.signum() > 0;
            case OUT -> quantity.signum() < 0;
            case ANY -> quantity.signum() != 0;
        };
    }

    private enum Sign {
        IN,
        OUT,
        ANY
    }
}
