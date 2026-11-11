package org.vvs.valimbavaka.feature.invoice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * One line of the sales book (FAC-06), read from the view {@code sales_book_entries}: an invoice, or a credit note as
 * a line in negative (the invoice it reverses keeps its own amount, so the two lines net to zero). Read-only: it is
 * never written, the documents themselves are never modified (JNL-02).
 */
@Entity
@Immutable
@Table(name = "sales_book_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesBookEntry {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, updatable = false, length = 20)
    private SalesBookEntryType type;

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    /** Invoice reversed by a credit note; empty on an invoice line. */
    @Column(name = "invoice_number", updatable = false, length = 20)
    private @Nullable String invoiceNumber;

    @Column(name = "sale_number", nullable = false, updatable = false, length = 20)
    private String saleNumber;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "sales_channel_label", updatable = false, length = 100)
    private @Nullable String salesChannelLabel;

    @Column(name = "seller_name", updatable = false)
    private @Nullable String sellerName;

    @Column(name = "payment_method", nullable = false, updatable = false, length = 20)
    private String paymentMethod;

    /** Status of the invoice; empty on a credit note line. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", updatable = false, length = 20)
    private @Nullable OperationStatus status;

    /** Reason of the credit note; empty on an invoice line. */
    @Column(name = "reason", updatable = false, length = 500)
    private @Nullable String reason;

    @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal total;
}
