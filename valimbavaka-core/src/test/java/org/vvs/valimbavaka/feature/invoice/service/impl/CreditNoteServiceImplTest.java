package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.invoice.domain.CreditNote;
import org.vvs.valimbavaka.feature.invoice.domain.Invoice;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceDraft;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceLineDraft;
import org.vvs.valimbavaka.feature.invoice.dto.CreditNoteRDto;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.feature.invoice.mapper.CreditNoteMapper;
import org.vvs.valimbavaka.feature.invoice.repository.CreditNoteRepository;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceRepository;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class CreditNoteServiceImplTest {

    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID SALE_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 8);
    private static final Instant NOW = Instant.parse("2026-11-08T09:30:00Z");

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CreditNoteRepository creditNoteRepository;

    @Mock
    private CreditNoteMapper creditNoteMapper;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private SaleService saleService;

    @Mock
    private JournalService journalService;

    @Captor
    private ArgumentCaptor<CreditNote> saved;

    private CreditNoteServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CreditNoteServiceImpl(
                invoiceRepository,
                creditNoteRepository,
                creditNoteMapper,
                invoiceService,
                saleService,
                journalService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issue_validatedInvoice_reversesItNumbersTheCreditNoteCancelsTheSaleAndTracesIt() {
        // Arrange
        var invoice = invoice();
        stubNewCreditNote(invoice, 7L);
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(OperationStatus.VALIDATED));
        var expected = view("AV-2026-0007");
        when(creditNoteMapper.toRDto(any(CreditNote.class))).thenReturn(expected);

        // Act
        var result = sut.issue(INVOICE_ID, "  Produit défectueux  ");

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(invoice.getStatus()).isEqualTo(OperationStatus.REVERSED);
        verify(creditNoteRepository).save(saved.capture());
        assertThat(saved.getValue().getNumber()).isEqualTo("AV-2026-0007");
        assertThat(saved.getValue().getReason()).isEqualTo("Produit défectueux");
        assertThat(saved.getValue().getIssuedAt()).isEqualTo(NOW);
        verify(saleService).cancel(SALE_ID, "avoir AV-2026-0007 : Produit défectueux");
        verify(journalService)
                .record(
                        eq(JournalAction.REVERSE),
                        eq("INVOICE"),
                        any(),
                        eq("Avoir AV-2026-0007 de la facture FAC-2026-0042 · vente VTE-0042 · 17000.00 Ar"
                                + " : Produit défectueux"));
    }

    @Test
    void issue_invoiceAlreadyReversed_throwsConflictAndUsesNoNumber() {
        // Arrange
        var invoice = invoice();
        invoice.reverse();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        // Act + Assert
        assertThatThrownBy(() -> sut.issue(INVOICE_ID, "Erreur de caisse")).isInstanceOf(ConflictException.class);
        verify(creditNoteRepository, never()).nextNumber(anyInt());
        verify(saleService, never()).cancel(any(), anyString());
    }

    @Test
    void issue_unknownInvoice_throwsNotFound() {
        // Arrange
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.issue(INVOICE_ID, "Erreur de caisse")).isInstanceOf(NotFoundException.class);
        verify(creditNoteRepository, never()).nextNumber(anyInt());
    }

    @Test
    void issue_saleAlreadyCancelledByAHotCancellation_doesNotCancelItTwice() {
        // Arrange
        stubNewCreditNote(invoice(), 8L);
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(OperationStatus.CANCELLED));
        when(creditNoteMapper.toRDto(any(CreditNote.class))).thenReturn(view("AV-2026-0008"));

        // Act
        sut.issue(INVOICE_ID, "Livraison annulée avant FAC-04");

        // Assert
        verify(saleService, never()).cancel(any(), anyString());
    }

    @Test
    void issue_dayOfTheSaleClosed_letsTheConflictRollBackTheCreditNote() {
        // Arrange
        stubNewCreditNote(invoice(), 9L);
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(OperationStatus.VALIDATED));
        doThrow(new ConflictException("La vente ne peut plus être annulée : sa journée est clôturée."))
                .when(saleService)
                .cancel(eq(SALE_ID), anyString());

        // Act + Assert
        assertThatThrownBy(() -> sut.issue(INVOICE_ID, "Retour client")).isInstanceOf(ConflictException.class);
    }

    @Test
    void issueForCancelledSale_validatedInvoice_issuesTheCreditNoteWithAStandardReasonWithoutCancellingTheSale() {
        // Arrange
        var invoice = invoice();
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.of(invoice));
        when(creditNoteRepository.nextNumber(2026)).thenReturn(10L);
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.issueForCancelledSale(SALE_ID);

        // Assert
        assertThat(invoice.getStatus()).isEqualTo(OperationStatus.REVERSED);
        verify(creditNoteRepository).save(saved.capture());
        assertThat(saved.getValue().getNumber()).isEqualTo("AV-2026-0010");
        assertThat(saved.getValue().getReason()).isEqualTo("Annulation de la vente VTE-0042");
        verify(saleService, never()).cancel(any(), anyString());
    }

    @Test
    void issueForCancelledSale_invoiceAlreadyReversed_doesNothing() {
        // Arrange
        var invoice = invoice();
        invoice.reverse();
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.of(invoice));

        // Act
        sut.issueForCancelledSale(SALE_ID);

        // Assert
        verify(creditNoteRepository, never()).nextNumber(anyInt());
        verify(creditNoteRepository, never()).save(any(CreditNote.class));
    }

    @Test
    void issueForCancelledSale_saleWithoutInvoice_doesNothing() {
        // Arrange
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.empty());

        // Act
        sut.issueForCancelledSale(SALE_ID);

        // Assert
        verify(creditNoteRepository, never()).nextNumber(anyInt());
    }

    @Test
    void getCreditNoteByInvoiceId_invoiceWithACreditNote_returnsIt() {
        // Arrange
        var expected = view("AV-2026-0007");
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenReturn(invoiceView());
        when(creditNoteRepository.findByInvoiceId(INVOICE_ID)).thenReturn(Optional.of(creditNote()));
        when(creditNoteMapper.toRDto(any(CreditNote.class))).thenReturn(expected);

        // Act
        var result = sut.getCreditNoteByInvoiceId(INVOICE_ID);

        // Assert
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getCreditNoteByInvoiceId_invoiceWithoutCreditNote_throwsNotFound() {
        // Arrange
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenReturn(invoiceView());
        when(creditNoteRepository.findByInvoiceId(INVOICE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getCreditNoteByInvoiceId(INVOICE_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCreditNoteByInvoiceId_invoiceNotReadable_letsTheNotFoundOfTheInvoiceThrough() {
        // Arrange
        when(invoiceService.getInvoiceById(INVOICE_ID)).thenThrow(new NotFoundException("Facture introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.getCreditNoteByInvoiceId(INVOICE_ID)).isInstanceOf(NotFoundException.class);
        verify(creditNoteRepository, never()).findByInvoiceId(any());
    }

    private void stubNewCreditNote(Invoice invoice, long sequence) {
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(creditNoteRepository.nextNumber(2026)).thenReturn(sequence);
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static CreditNote creditNote() {
        return CreditNote.issue("AV-2026-0007", invoice(), "Produit défectueux", NOW);
    }

    private static Invoice invoice() {
        var amount = new BigDecimal("17000.00");
        var draft = new InvoiceDraft(
                SALE_ID,
                "VTE-0042",
                Instant.parse("2026-11-08T07:00:00Z"),
                DAY,
                "Boutique",
                UUID.randomUUID(),
                "Miora Rakoto",
                "MVOLA",
                "By Valimbavaka Cosmetics",
                null,
                amount,
                new BigDecimal("0.00"),
                amount,
                List.of(new InvoiceLineDraft(
                        UUID.randomUUID(), "Savon coco", new BigDecimal("2.000"), new BigDecimal("8500.00"), amount)));
        return Invoice.issue("FAC-2026-0042", draft);
    }

    private static SaleRDto sale(OperationStatus status) {
        return new SaleRDto(
                SALE_ID,
                "VTE-0042",
                UUID.randomUUID(),
                DAY,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentMethod.MVOLA,
                status,
                new BigDecimal("17000.00"),
                new BigDecimal("0.00"),
                null,
                new BigDecimal("17000.00"),
                List.of(),
                Instant.parse("2026-11-08T07:00:00Z"),
                null);
    }

    private static InvoiceRDto invoiceView() {
        var amount = new BigDecimal("17000.00");
        return new InvoiceRDto(
                INVOICE_ID,
                "FAC-2026-0042",
                SALE_ID,
                "VTE-0042",
                Instant.parse("2026-11-08T07:00:00Z"),
                DAY,
                OperationStatus.VALIDATED,
                "Boutique",
                null,
                null,
                "MVOLA",
                "By Valimbavaka Cosmetics",
                null,
                amount,
                new BigDecimal("0.00"),
                amount,
                List.of());
    }

    private static CreditNoteRDto view(String number) {
        var amount = new BigDecimal("17000.00");
        return new CreditNoteRDto(
                UUID.randomUUID(),
                number,
                INVOICE_ID,
                "FAC-2026-0042",
                SALE_ID,
                "VTE-0042",
                NOW,
                DAY,
                "Produit défectueux",
                "By Valimbavaka Cosmetics",
                null,
                amount,
                new BigDecimal("0.00"),
                amount,
                List.of());
    }
}
