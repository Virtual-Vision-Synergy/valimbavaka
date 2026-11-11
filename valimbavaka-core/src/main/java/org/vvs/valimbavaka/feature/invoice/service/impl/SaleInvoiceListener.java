package org.vvs.valimbavaka.feature.invoice.service.impl;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.feature.sale.domain.SaleRecordedEvent;

/**
 * Issues the invoice of every recorded sale (FAC-01). A plain synchronous listener on purpose: it runs in the
 * transaction of the sale, so there is never a sale without an invoice, and if the invoice fails the sale is rolled
 * back with it. The sale does not know the invoice feature (no cycle).
 */
@Component
class SaleInvoiceListener {

    private final InvoiceService invoiceService;

    SaleInvoiceListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @EventListener
    void onSaleRecorded(SaleRecordedEvent event) {
        invoiceService.issueForSale(event.saleId());
    }
}
