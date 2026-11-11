package org.vvs.valimbavaka.feature.invoice.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.invoice.dto.InvoiceLineRDto;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceDocumentService;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceFile;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.shared.excel.PdfExporter;

@Service
@Transactional(readOnly = true)
public class InvoiceDocumentServiceImpl implements InvoiceDocumentService {

    private final InvoiceService invoiceService;
    private final PdfExporter pdfExporter;
    private final InvoicePdfLayout layout;

    InvoiceDocumentServiceImpl(InvoiceService invoiceService, PdfExporter pdfExporter, InvoicePdfLayout layout) {
        this.invoiceService = invoiceService;
        this.pdfExporter = pdfExporter;
        this.layout = layout;
    }

    @Override
    public InvoiceFile renderPdf(UUID invoiceId) {
        var invoice = invoiceService.getInvoiceById(invoiceId);
        var report = PdfExporter.report(layout.title(invoice), InvoiceLineRDto.class, invoice.lines().stream())
                .withDetails(layout.details(invoice))
                .withSummary(layout.summary(invoice));
        var out = new ByteArrayOutputStream();
        try {
            pdfExporter.export(report, out);
        } catch (IOException e) {
            throw new UncheckedIOException("PDF de la facture " + invoice.number() + " impossible", e);
        }
        return new InvoiceFile(invoice.number() + ".pdf", new ByteArrayResource(out.toByteArray()));
    }
}
