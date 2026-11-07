package org.vvs.valimbavaka.feature.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Line of a restock order: written once with the order, never modified. Article, supplier and dates are a copy taken
 * at generation time, so the order stays readable if the article or the supplier changes afterwards.
 */
@Entity
@Immutable
@Table(name = "restock_order_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestockOrderLine {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "restock_order_id", nullable = false, updatable = false)
    private UUID restockOrderId;

    @Column(name = "line_number", nullable = false, updatable = false)
    private int lineNumber;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "article_code", nullable = false, updatable = false, length = 20)
    private String articleCode;

    @Column(name = "article_name", nullable = false, updatable = false, length = 120)
    private String articleName;

    @Column(name = "unit", nullable = false, updatable = false, length = 20)
    private String unit;

    @Column(name = "stock_quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal stockQuantity;

    @Column(name = "threshold", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal threshold;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "supplier_id", updatable = false)
    private @Nullable UUID supplierId;

    @Column(name = "supplier_name", updatable = false, length = 150)
    private @Nullable String supplierName;

    @Column(name = "lead_time_days", updatable = false)
    private @Nullable Integer leadTimeDays;

    @Column(name = "expected_delivery_date", updatable = false)
    private @Nullable LocalDate expectedDeliveryDate;

    /** @param lineNumber position of the line in the order, from 1 */
    public static RestockOrderLine of(UUID restockOrderId, int lineNumber, RestockOrderLineCommand command) {
        var line = new RestockOrderLine();
        line.restockOrderId = restockOrderId;
        line.lineNumber = lineNumber;
        line.articleId = command.articleId();
        line.articleCode = command.articleCode();
        line.articleName = command.articleName();
        line.unit = command.unit();
        line.stockQuantity = command.stockQuantity();
        line.threshold = command.threshold();
        line.quantity = command.quantity();
        line.supplierId = command.supplierId();
        line.supplierName = command.supplierName();
        line.leadTimeDays = command.leadTimeDays();
        line.expectedDeliveryDate = command.expectedDeliveryDate();
        return line;
    }
}
