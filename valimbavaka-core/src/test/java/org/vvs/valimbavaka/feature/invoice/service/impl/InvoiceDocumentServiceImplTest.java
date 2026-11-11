package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceLineRDto;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.shared.excel.PdfExporter;
import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class InvoiceDocumentServiceImplTest {

    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final ExcelProperties PROPERTIES = new ExcelProperties(
            200,
            new ExcelProperties.Column(40, 2),
            new ExcelProperties.Format("dd/mm/yyyy", "dd/mm/yyyy hh:mm"),
            ZoneId.of("Indian/Antananarivo"));

    @Mock
    private InvoiceService invoiceService;

    private InvoiceDocumentServiceImpl sut;

    @BeforeEach
    void setUp() {
        var exporter = new PdfExporter(PROPERTIES, Clock.fixed(Instant.parse("2026-11-08T08:00:00Z"), ZoneOffset.UTC));
        sut = new InvoiceDocumentServiceImpl(invoiceService, exporter, new InvoicePdfLayout(PROPERTIES));
    }

    @Test
    void renderPdf_issuedInvoice_isNamedAfterItsNumberAndPrintsTheFrozenContent() throws IOException {
        // Arrange
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenReturn(invoice(OperationStatus.VALIDATED, "0", "25500"));

        // Act
        var file = sut.renderPdf(INVOICE_ID);

        // Assert
        assertThat(file.filename()).isEqualTo("FAC-2026-0042.pdf");
        assertThat(file.content().contentLength()).isPositive();
        var text = textOf(file.content().getContentAsByteArray());
        assertThat(text)
                .contains("Facture FAC-2026-0042")
                .contains("By Valimbavaka Cosmetics")
                .contains("NIF : 4001234567")
                .contains("Émise le 08/11/2026 10:52") // 07:52 UTC, heure de la boutique
                .contains("journée du 08/11/2026")
                .contains("vente VTE-0042")
                .contains("Vendeuse : Miora Rakoto")
                .contains("canal : Boutique")
                .contains("règlement : MVola")
                .contains("Savon coco")
                .contains("8 500")
                .contains("Total à payer : 25 500 Ar")
                .doesNotContain("Remise")
                .doesNotContain("Statut");
    }

    @Test
    void renderPdf_invoiceWithADiscount_printsSubtotalDiscountAndTotal() throws IOException {
        // Arrange
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenReturn(invoice(OperationStatus.VALIDATED, "2550", "22950"));

        // Act
        var text = textOf(sut.renderPdf(INVOICE_ID).content().getContentAsByteArray());

        // Assert
        assertThat(text)
                .contains("Sous-total : 25 500 Ar")
                .contains("Remise : -2 550 Ar")
                .contains("Total à payer : 22 950 Ar");
    }

    @Test
    void renderPdf_nifNotConfiguredAndNoSeller_printsPlaceholders() throws IOException {
        // Arrange
        var base = invoice(OperationStatus.VALIDATED, "0", "25500");
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenReturn(withoutNifAndSeller(base));

        // Act
        var text = textOf(sut.renderPdf(INVOICE_ID).content().getContentAsByteArray());

        // Assert
        assertThat(text).contains("NIF : [à compléter]").contains("Vendeuse : -");
    }

    @Test
    void renderPdf_cancelledInvoice_printsItsStatus() throws IOException {
        // Arrange
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenReturn(invoice(OperationStatus.CANCELLED, "0", "25500"));

        // Act
        var text = textOf(sut.renderPdf(INVOICE_ID).content().getContentAsByteArray());

        // Assert
        assertThat(text).contains("Statut : annulée");
    }

    @Test
    void renderPdf_unknownInvoice_propagatesNotFound() {
        // Arrange
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenThrow(new NotFoundException("Facture introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.renderPdf(INVOICE_ID)).isInstanceOf(NotFoundException.class);
    }

    private static InvoiceRDto invoice(OperationStatus status, String discount, String total) {
        var line = new InvoiceLineRDto(
                UUID.randomUUID(), "Savon coco", new BigDecimal("3"), new BigDecimal("8500"), new BigDecimal("25500"));
        return new InvoiceRDto(
                INVOICE_ID,
                "FAC-2026-0042",
                UUID.randomUUID(),
                "VTE-0042",
                Instant.parse("2026-11-08T07:52:00Z"),
                LocalDate.of(2026, 11, 8),
                status,
                "Boutique",
                UUID.randomUUID(),
                "Miora Rakoto",
                "MVOLA",
                "By Valimbavaka Cosmetics",
                "4001234567",
                new BigDecimal("25500"),
                new BigDecimal(discount),
                new BigDecimal(total),
                List.of(line));
    }

    private static InvoiceRDto withoutNifAndSeller(InvoiceRDto base) {
        return new InvoiceRDto(
                base.id(),
                base.number(),
                base.saleId(),
                base.saleNumber(),
                base.issuedAt(),
                base.businessDate(),
                base.status(),
                base.salesChannelLabel(),
                null,
                null,
                base.paymentMethod(),
                base.issuerName(),
                null,
                base.subtotal(),
                base.discountAmount(),
                base.total(),
                base.lines());
    }

    private static String textOf(byte[] pdf) throws IOException {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
