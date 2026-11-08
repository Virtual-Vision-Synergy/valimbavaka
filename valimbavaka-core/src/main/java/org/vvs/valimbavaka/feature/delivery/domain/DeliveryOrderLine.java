package org.vvs.valimbavaka.feature.delivery.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Article, quantity and unit price of an order line. The price is copied from the article at creation time, so a
 * later price change does not alter the amount to collect; the article is referenced by id (no JPA association).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryOrderLine {

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    static DeliveryOrderLine of(DeliveryOrderLineCommand command) {
        var line = new DeliveryOrderLine();
        line.articleId = command.articleId();
        line.quantity = command.quantity();
        line.unitPrice = command.unitPrice();
        return line;
    }

    /** Quantity × unit price, rounded to 2 decimals (half up). */
    public BigDecimal getAmount() {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }
}
