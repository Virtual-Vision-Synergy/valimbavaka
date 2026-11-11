package org.vvs.valimbavaka.feature.invoice.service.impl;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.invoice.service.CreditNoteService;
import org.vvs.valimbavaka.feature.sale.domain.SaleCancelledEvent;

/**
 * Issues the credit note of the invoice of every sale cancelled elsewhere (FAC-04, undo of a delivery LIV-05): an
 * issued invoice is never left {@code VALIDATED} for a cancelled sale. A plain synchronous listener, in the
 * transaction of the cancellation; the sale does not know the invoice feature (no cycle).
 */
@Component
class SaleCancelledListener {

    private final CreditNoteService creditNoteService;

    SaleCancelledListener(CreditNoteService creditNoteService) {
        this.creditNoteService = creditNoteService;
    }

    @EventListener
    void onSaleCancelled(SaleCancelledEvent event) {
        creditNoteService.issueForCancelledSale(event.saleId());
    }
}
