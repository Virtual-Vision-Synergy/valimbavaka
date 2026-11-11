package org.vvs.valimbavaka.feature.invoice.service.impl;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.invoice.domain.CreditNote;
import org.vvs.valimbavaka.feature.invoice.domain.Invoice;
import org.vvs.valimbavaka.feature.invoice.dto.CreditNoteRDto;
import org.vvs.valimbavaka.feature.invoice.mapper.CreditNoteMapper;
import org.vvs.valimbavaka.feature.invoice.repository.CreditNoteRepository;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceRepository;
import org.vvs.valimbavaka.feature.invoice.service.CreditNoteService;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@Service
@Transactional(readOnly = true)
public class CreditNoteServiceImpl implements CreditNoteService {

    static final String RESOURCE_TYPE = "INVOICE";

    private static final Logger log = LoggerFactory.getLogger(CreditNoteServiceImpl.class);

    private final InvoiceRepository invoiceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final CreditNoteMapper creditNoteMapper;
    private final InvoiceService invoiceService;
    private final SaleService saleService;
    private final JournalService journalService;
    private final Clock clock;

    CreditNoteServiceImpl(
            InvoiceRepository invoiceRepository,
            CreditNoteRepository creditNoteRepository,
            CreditNoteMapper creditNoteMapper,
            InvoiceService invoiceService,
            SaleService saleService,
            JournalService journalService,
            Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.creditNoteMapper = creditNoteMapper;
        this.invoiceService = invoiceService;
        this.saleService = saleService;
        this.journalService = journalService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreditNoteRDto issue(UUID invoiceId, String reason) {
        var invoice = invoiceRepository
                .findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture introuvable : " + invoiceId));
        var cleanReason = reason.strip();
        // The invoice is reversed first: the listener of the cancellation below then finds it already credited.
        var creditNote = reverse(invoice, cleanReason);
        cancelSaleIfStillValidated(invoice, "avoir %s : %s".formatted(creditNote.getNumber(), cleanReason));
        return creditNoteMapper.toRDto(creditNote);
    }

    @Override
    @Transactional
    public void issueForCancelledSale(UUID saleId) {
        invoiceRepository
                .findBySaleId(saleId)
                .filter(invoice -> invoice.getStatus() == OperationStatus.VALIDATED)
                .ifPresent(invoice -> reverse(invoice, "Annulation de la vente " + invoice.getSaleNumber()));
    }

    @Override
    public CreditNoteRDto getCreditNoteByInvoiceId(UUID invoiceId) {
        var invoice = invoiceService.getInvoiceById(invoiceId);
        return creditNoteRepository
                .findByInvoiceId(invoice.id())
                .map(creditNoteMapper::toRDto)
                .orElseThrow(() -> new NotFoundException("Aucun avoir pour la facture : " + invoice.number()));
    }

    /** Invoice to {@code REVERSED} (409 if it is not validated, before any number is used), credit note, journal. */
    private CreditNote reverse(Invoice invoice, String reason) {
        invoice.reverse();
        invoiceRepository.save(invoice);
        var year = invoice.getBusinessDate().getYear();
        var number = "AV-%d-%04d".formatted(year, creditNoteRepository.nextNumber(year));
        var creditNote = creditNoteRepository.save(CreditNote.issue(number, invoice, reason, clock.instant()));
        journalService.record(
                JournalAction.REVERSE,
                RESOURCE_TYPE,
                invoice.getId(),
                "Avoir %s de la facture %s · vente %s · %s Ar : %s"
                        .formatted(
                                number,
                                invoice.getNumber(),
                                invoice.getSaleNumber(),
                                invoice.getTotal().toPlainString(),
                                reason));
        log.info("Avoir {} émis pour la facture {}", number, invoice.getNumber());
        return creditNote;
    }

    /** A sale already cancelled by a hot cancellation (before FAC-04) has had its stock given back: not twice. */
    private void cancelSaleIfStillValidated(Invoice invoice, String reason) {
        var sale = saleService.getSaleById(invoice.getSaleId());
        if (sale.status() == OperationStatus.VALIDATED) {
            saleService.cancel(sale.id(), reason);
        }
    }
}
