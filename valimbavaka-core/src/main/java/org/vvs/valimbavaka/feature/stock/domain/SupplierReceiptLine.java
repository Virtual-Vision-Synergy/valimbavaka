package org.vvs.valimbavaka.feature.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Received article, quantity and unit cost; the article is referenced by id, its code and name are copied. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierReceiptLine {

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "article_code", nullable = false, updatable = false, length = 20)
    private String articleCode;

    @Column(name = "article_name", nullable = false, updatable = false, length = 120)
    private String articleName;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal unitCost;

    static SupplierReceiptLine of(SupplierReceiptLineCommand command) {
        var line = new SupplierReceiptLine();
        line.articleId = command.articleId();
        line.articleCode = command.articleCode();
        line.articleName = command.articleName();
        line.quantity = command.quantity();
        line.unitCost = command.unitCost();
        return line;
    }
}
