package org.vvs.valimbavaka.feature.sale.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Article, quantity and prices of a sale line, frozen when the sale is recorded; the article is referenced by id. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleLine {

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit_cost", updatable = false, precision = 19, scale = 2)
    private @Nullable BigDecimal unitCost;

    @Column(name = "line_total", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    /** The line total is the unit price times the quantity, rounded half up to 2 decimals. */
    static SaleLine of(PricedSaleLine line) {
        var saleLine = new SaleLine();
        saleLine.articleId = line.articleId();
        saleLine.quantity = line.quantity();
        saleLine.unitPrice = line.unitPrice().setScale(2, RoundingMode.HALF_UP);
        saleLine.unitCost = line.unitCost();
        saleLine.lineTotal = saleLine.unitPrice.multiply(line.quantity()).setScale(2, RoundingMode.HALF_UP);
        return saleLine;
    }
}
