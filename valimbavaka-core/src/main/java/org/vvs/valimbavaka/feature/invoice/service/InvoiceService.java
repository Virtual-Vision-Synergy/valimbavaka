package org.vvs.valimbavaka.feature.invoice.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;

/**
 * Invoices (FAC-01). An invoice is issued by the listener of {@code SaleRecordedEvent}, in the transaction of the sale:
 * no sale without its invoice, and a sale rolled back gives its invoice number back.
 */
public interface InvoiceService {

    /**
     * Issues the invoice of a recorded sale, numbered {@code FAC-<year of the business day>-<NNNN>} without gap, and
     * traces it in the journal ({@code CREATE}). Idempotent: a sale that already has its invoice gets it back, no
     * second number is used. 404 if the sale is unknown. Must be called in the transaction of the sale.
     */
    InvoiceRDto issueForSale(UUID saleId);

    /**
     * Regenerates (re-edits) the invoice of an existing sale (FAC-02): the same invoice comes back, same number, same
     * lines, amounts and payment method (a snapshot never changes, no number used), and the re-edition is traced in
     * the journal ({@code UPDATE}). A sale recorded before FAC-01 that has no invoice yet gets it issued (catch-up,
     * {@code CREATE}). Never creates an invoice that is not tied to a recorded sale: 404 if the sale is unknown, or
     * belongs to another seller than the active seller who asks.
     */
    InvoiceRDto regenerateForSale(UUID saleId);

    /** One invoice (404 if unknown, or if it belongs to another seller than the active seller who asks). */
    InvoiceRDto getInvoiceById(UUID invoiceId);

    /** The invoice of a sale (same 404 rules as {@link #getInvoiceById}). */
    InvoiceRDto getInvoiceBySaleId(UUID saleId);
}
