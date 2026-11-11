package org.vvs.valimbavaka.feature.invoice.service;

import java.util.UUID;

/** Printable documents made from an issued invoice (FAC-03). They never modify the invoice. */
public interface InvoiceDocumentService {

    /**
     * The PDF of an invoice, named {@code <number>.pdf}: issuer, number, dates, seller, payment method, the frozen
     * lines and the amounts, exactly as issued (never recomputed from the sale or the sheets). The same 404 rules as
     * {@link InvoiceService#getInvoiceById}: unknown invoice, or invoice of another seller than the active seller.
     */
    InvoiceFile renderPdf(UUID invoiceId);
}
