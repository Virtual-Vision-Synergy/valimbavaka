package org.vvs.valimbavaka.feature.invoice.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Article name, quantity and prices of an invoice line, frozen when the invoice is issued. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceLine {

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "label", nullable = false, updatable = false)
    private String label;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    static InvoiceLine of(InvoiceLineDraft draft) {
        var line = new InvoiceLine();
        line.articleId = draft.articleId();
        line.label = draft.label();
        line.quantity = draft.quantity();
        line.unitPrice = draft.unitPrice();
        line.lineTotal = draft.lineTotal();
        return line;
    }
}
