package org.vvs.valimbavaka.feature.invoice.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.invoice.dto.CreditNoteRDto;

/**
 * Credit notes (FAC-04): the only way to undo an issued invoice, which is never modified nor deleted. A credit note
 * is numbered {@code AV-<year of the business day>-<NNNN>} without gap, carries a reason, puts the invoice in the
 * {@code REVERSED} status and, with it, cancels the sale (the articles come back into stock).
 */
public interface CreditNoteService {

    /**
     * Issues the credit note of an invoice on request of the administrator, for a reason (required, stripped).
     * Cancels the sale through {@code SaleService.cancel} in the same transaction (stock, promotion, journal), then
     * journals the reversal of the invoice ({@code REVERSE}). A sale that was already cancelled (hot cancellation
     * before FAC-04) is not cancelled a second time. 404 if the invoice is unknown; 409 if it is no longer
     * {@code VALIDATED} (already credited), or if the day of the sale is closed.
     */
    CreditNoteRDto issue(UUID invoiceId, String reason);

    /**
     * Issues the credit note of the invoice of a sale that has just been cancelled elsewhere (undo of a delivery,
     * LIV-05), with a standard reason. Does nothing if the sale has no invoice (sold before FAC-01) or if the invoice
     * is no longer {@code VALIDATED} (credit note already issued by {@link #issue}). Must be called in the transaction
     * of the cancellation.
     */
    void issueForCancelledSale(UUID saleId);

    /** The credit note of an invoice (same 404 rules as the invoice itself; 404 if it has none). */
    CreditNoteRDto getCreditNoteByInvoiceId(UUID invoiceId);
}
