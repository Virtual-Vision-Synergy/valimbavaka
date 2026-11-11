package org.vvs.valimbavaka.feature.invoice.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.invoice.domain.Invoice;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.feature.invoice.mapper.InvoiceMapper;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceRepository;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.user.service.UserService;

@Service
@Transactional(readOnly = true)
public class InvoiceServiceImpl implements InvoiceService {

    static final String RESOURCE_TYPE = "INVOICE";

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceDrafts drafts;
    private final SaleService saleService;
    private final JournalService journalService;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;

    InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            InvoiceDrafts drafts,
            SaleService saleService,
            JournalService journalService,
            UserService userService,
            AuditorAware<UUID> auditorAware) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.drafts = drafts;
        this.saleService = saleService;
        this.journalService = journalService;
        this.userService = userService;
        this.auditorAware = auditorAware;
    }

    @Override
    @Transactional
    public InvoiceRDto issueForSale(UUID saleId) {
        var existing = invoiceRepository.findBySaleId(saleId);
        if (existing.isPresent()) {
            return invoiceMapper.toRDto(existing.get());
        }
        var sale = saleService.getSaleById(saleId);
        var draft = drafts.of(sale);
        var year = draft.businessDate().getYear();
        var number = "FAC-%d-%04d".formatted(year, invoiceRepository.nextNumber(year));
        var invoice = invoiceRepository.save(Invoice.issue(number, draft));
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, invoice.getId(), description(number, sale));
        log.info("Facture {} émise pour la vente {}", number, sale.number());
        return invoiceMapper.toRDto(invoice);
    }

    @Override
    public InvoiceRDto getInvoiceById(UUID invoiceId) {
        var invoice = invoiceRepository
                .findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Facture introuvable : " + invoiceId));
        return invoiceMapper.toRDto(readable(invoice));
    }

    @Override
    public InvoiceRDto getInvoiceBySaleId(UUID saleId) {
        var invoice = invoiceRepository
                .findBySaleId(saleId)
                .orElseThrow(() -> new NotFoundException("Facture introuvable pour la vente : " + saleId));
        return invoiceMapper.toRDto(readable(invoice));
    }

    /** An active seller only reads the invoices of the sales credited to her (as for the sales list, VTE-07). */
    private Invoice readable(Invoice invoice) {
        var own = auditorAware.getCurrentAuditor().filter(userService::isActiveSeller);
        if (own.isPresent() && !own.get().equals(invoice.getSellerId())) {
            throw new NotFoundException("Facture introuvable : " + invoice.getId());
        }
        return invoice;
    }

    /** E.g. {@code Facture FAC-2026-0042 · vente VTE-0042 · 64000.00 Ar · Espèces}. */
    private static String description(String number, SaleRDto sale) {
        return "Facture %s · vente %s · %s Ar · %s"
                .formatted(
                        number,
                        sale.number(),
                        sale.total().toPlainString(),
                        sale.paymentMethod().label());
    }
}
