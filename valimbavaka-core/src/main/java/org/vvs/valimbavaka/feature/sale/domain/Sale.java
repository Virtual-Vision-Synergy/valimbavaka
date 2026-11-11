package org.vvs.valimbavaka.feature.sale.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Sale of finished articles (VTE-01) on a sales channel, credited to a seller, within an opened business day. Prices
 * are frozen on the lines; the stock leaves the location of the channel through {@code SALE} movements referencing
 * the sale. A validated sale is never deleted: it is cancelled or reversed (JNL-02). The author who entered it is
 * {@code createdBy}.
 */
@Entity
@Table(name = "sales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sale extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "business_day_id", nullable = false, updatable = false)
    private UUID businessDayId;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "sales_channel_id", nullable = false, updatable = false)
    private UUID salesChannelId;

    @Column(name = "stock_location_id", nullable = false, updatable = false)
    private UUID stockLocationId;

    /** Seller credited with the sale; {@code null} only for a sale created by another feature without one. */
    @Column(name = "seller_id", updatable = false)
    private @Nullable UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, updatable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OperationStatus status;

    @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @ElementCollection
    @CollectionTable(name = "sale_lines", joinColumns = @JoinColumn(name = "sale_id"))
    @OrderColumn(name = "line_number")
    private List<SaleLine> lines = new ArrayList<>();

    /**
     * A validated sale: subtotal = sum of the line totals; no discount yet (promotions: PRO-02, VTE-04), so the total
     * equals the subtotal.
     */
    public static Sale record(String number, SaleHeader header, List<PricedSaleLine> lines) {
        var sale = new Sale();
        sale.number = number;
        sale.businessDayId = header.businessDayId();
        sale.businessDate = header.businessDate();
        sale.salesChannelId = header.salesChannelId();
        sale.stockLocationId = header.stockLocationId();
        sale.sellerId = header.sellerId();
        sale.paymentMethod = header.paymentMethod();
        sale.status = OperationStatus.VALIDATED;
        lines.forEach(line -> sale.lines.add(SaleLine.of(line)));
        sale.subtotal = sale.lines.stream().map(SaleLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        sale.discountAmount = new BigDecimal("0.00");
        sale.total = sale.subtotal.subtract(sale.discountAmount);
        return sale;
    }

    /** Lines in the order they were entered; they never change once recorded. */
    public List<SaleLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
