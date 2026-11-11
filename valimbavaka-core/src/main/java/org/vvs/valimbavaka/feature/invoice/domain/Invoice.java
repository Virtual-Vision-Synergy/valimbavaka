package org.vvs.valimbavaka.feature.invoice.domain;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Invoice issued when a sale is recorded (FAC-01): a legal document, never modified nor deleted (JNL-02). Everything
 * printed on it is a snapshot taken at issue time; the sale is referenced by id only (no association between
 * features). Only the status can change later (credit note, FAC-04).
 */
@Entity
@Table(name = "invoices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "sale_id", nullable = false, updatable = false)
    private UUID saleId;

    @Column(name = "sale_number", nullable = false, updatable = false, length = 20)
    private String saleNumber;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "sales_channel_label", updatable = false, length = 100)
    private @Nullable String salesChannelLabel;

    @Column(name = "seller_id", updatable = false)
    private @Nullable UUID sellerId;

    @Column(name = "seller_name", updatable = false)
    private @Nullable String sellerName;

    @Column(name = "payment_method", nullable = false, updatable = false, length = 20)
    private String paymentMethod;

    @Column(name = "issuer_name", nullable = false, updatable = false, length = 150)
    private String issuerName;

    @Column(name = "issuer_nif", updatable = false, length = 50)
    private @Nullable String issuerNif;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OperationStatus status;

    @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @ElementCollection
    @CollectionTable(name = "invoice_lines", joinColumns = @JoinColumn(name = "invoice_id"))
    @OrderColumn(name = "line_number")
    private List<InvoiceLine> lines = new ArrayList<>();

    /** A validated invoice carrying the given number, the sale's amounts and one line per sale line. */
    public static Invoice issue(String number, InvoiceDraft draft) {
        var invoice = new Invoice();
        invoice.number = number;
        invoice.saleId = draft.saleId();
        invoice.saleNumber = draft.saleNumber();
        invoice.issuedAt = draft.issuedAt();
        invoice.businessDate = draft.businessDate();
        invoice.salesChannelLabel = draft.salesChannelLabel();
        invoice.sellerId = draft.sellerId();
        invoice.sellerName = draft.sellerName();
        invoice.paymentMethod = draft.paymentMethod();
        invoice.issuerName = draft.issuerName();
        invoice.issuerNif = draft.issuerNif();
        invoice.status = OperationStatus.VALIDATED;
        invoice.subtotal = draft.subtotal();
        invoice.discountAmount = draft.discountAmount();
        invoice.total = draft.total();
        draft.lines().forEach(line -> invoice.lines.add(InvoiceLine.of(line)));
        return invoice;
    }

    /** Lines in the order of the sale; they never change once issued. */
    public List<InvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
