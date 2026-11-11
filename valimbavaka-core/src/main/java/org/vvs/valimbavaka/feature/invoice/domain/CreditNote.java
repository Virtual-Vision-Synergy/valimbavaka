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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Credit note (avoir) of an invoice (FAC-04): the reversal of an issued invoice, with its own gapless number and the
 * reason that justifies it. A frozen copy of the invoice (issuer, amounts, lines): never modified nor deleted (JNL-02).
 * The invoice and the sale are referenced by id only (no association between features); an invoice has one credit
 * note at most.
 */
@Entity
@Table(name = "credit_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditNote extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Column(name = "invoice_number", nullable = false, updatable = false, length = 20)
    private String invoiceNumber;

    @Column(name = "sale_id", nullable = false, updatable = false)
    private UUID saleId;

    @Column(name = "sale_number", nullable = false, updatable = false, length = 20)
    private String saleNumber;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "issuer_name", nullable = false, updatable = false, length = 150)
    private String issuerName;

    @Column(name = "issuer_nif", updatable = false, length = 50)
    private @Nullable String issuerNif;

    @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @ElementCollection
    @CollectionTable(name = "credit_note_lines", joinColumns = @JoinColumn(name = "credit_note_id"))
    @OrderColumn(name = "line_number")
    private List<InvoiceLine> lines = new ArrayList<>();

    /** The credit note of the whole invoice: same issuer, same amounts, one line per invoice line. */
    public static CreditNote issue(String number, Invoice invoice, String reason, Instant issuedAt) {
        var creditNote = new CreditNote();
        creditNote.number = number;
        creditNote.invoiceId = invoice.getId();
        creditNote.invoiceNumber = invoice.getNumber();
        creditNote.saleId = invoice.getSaleId();
        creditNote.saleNumber = invoice.getSaleNumber();
        creditNote.issuedAt = issuedAt;
        creditNote.businessDate = invoice.getBusinessDate();
        creditNote.reason = reason;
        creditNote.issuerName = invoice.getIssuerName();
        creditNote.issuerNif = invoice.getIssuerNif();
        creditNote.subtotal = invoice.getSubtotal();
        creditNote.discountAmount = invoice.getDiscountAmount();
        creditNote.total = invoice.getTotal();
        invoice.getLines().forEach(line -> creditNote.lines.add(copyOf(line)));
        return creditNote;
    }

    private static InvoiceLine copyOf(InvoiceLine line) {
        return InvoiceLine.of(new InvoiceLineDraft(
                line.getArticleId(), line.getLabel(), line.getQuantity(), line.getUnitPrice(), line.getLineTotal()));
    }

    /** Lines in the order of the invoice; they never change once issued. */
    public List<InvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
