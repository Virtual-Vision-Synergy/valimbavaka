package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.data.domain.AuditorAware;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.invoice.domain.Invoice;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceDraft;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceLineDraft;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.feature.invoice.mapper.InvoiceMapper;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID COLLEAGUE = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 8);

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private InvoiceDrafts drafts;

    @Mock
    private SaleService saleService;

    @Mock
    private JournalService journalService;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    @Captor
    private ArgumentCaptor<Invoice> saved;

    private InvoiceServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new InvoiceServiceImpl(
                invoiceRepository, invoiceMapper, drafts, saleService, journalService, userService, auditorAware);
    }

    @Test
    void issueForSale_newSale_numbersTheInvoiceWithTheYearOfTheBusinessDayAndTracesIt() {
        // Arrange
        var sale = sale();
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.empty());
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale);
        when(drafts.of(sale)).thenReturn(draft(SELLER));
        when(invoiceRepository.nextNumber(2026)).thenReturn(42L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));
        var expected = view("FAC-2026-0042");
        when(invoiceMapper.toRDto(any(Invoice.class))).thenReturn(expected);

        // Act
        var result = sut.issueForSale(SALE_ID);

        // Assert
        assertThat(result).isSameAs(expected);
        verify(invoiceRepository).save(saved.capture());
        assertThat(saved.getValue().getNumber()).isEqualTo("FAC-2026-0042");
        assertThat(saved.getValue().getStatus()).isEqualTo(OperationStatus.VALIDATED);
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("INVOICE"),
                        any(),
                        eq("Facture FAC-2026-0042 · vente VTE-0042 · 17000.00 Ar · MVola"));
    }

    @Test
    void issueForSale_saleAlreadyInvoiced_returnsTheExistingInvoiceAndUsesNoNumber() {
        // Arrange
        var existing = Invoice.issue("FAC-2026-0007", draft(SELLER));
        var expected = view("FAC-2026-0007");
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.of(existing));
        when(invoiceMapper.toRDto(existing)).thenReturn(expected);

        // Act
        var result = sut.issueForSale(SALE_ID);

        // Assert
        assertThat(result).isSameAs(expected);
        verify(invoiceRepository, never()).nextNumber(anyInt());
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void getInvoiceBySaleId_activeSellerAskingForAColleagueInvoice_returns404() {
        // Arrange
        var invoice = Invoice.issue("FAC-2026-0008", draft(COLLEAGUE));
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.of(invoice));
        connectedAs(SELLER, true);

        // Act + Assert
        assertThatThrownBy(() -> sut.getInvoiceBySaleId(SALE_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getInvoiceBySaleId_activeSellerAskingForHerOwnInvoice_returnsIt() {
        // Arrange
        var invoice = Invoice.issue("FAC-2026-0009", draft(SELLER));
        var expected = view("FAC-2026-0009");
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toRDto(invoice)).thenReturn(expected);
        connectedAs(SELLER, true);

        // Act + Assert
        assertThat(sut.getInvoiceBySaleId(SALE_ID)).isSameAs(expected);
    }

    @Test
    void getInvoiceBySaleId_activeSellerAskingForAnInvoiceWithoutSeller_returns404() {
        // Arrange
        var invoice = Invoice.issue("FAC-2026-0010", draft(null));
        when(invoiceRepository.findBySaleId(SALE_ID)).thenReturn(Optional.of(invoice));
        connectedAs(SELLER, true);

        // Act + Assert
        assertThatThrownBy(() -> sut.getInvoiceBySaleId(SALE_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getInvoiceById_administrator_readsAnyInvoice() {
        // Arrange
        var invoice = Invoice.issue("FAC-2026-0011", draft(COLLEAGUE));
        var expected = view("FAC-2026-0011");
        var invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toRDto(invoice)).thenReturn(expected);
        connectedAs(ADMIN, false);

        // Act + Assert
        assertThat(sut.getInvoiceById(invoiceId)).isSameAs(expected);
    }

    @Test
    void getInvoiceById_unknownInvoice_returns404() {
        // Arrange
        var invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getInvoiceById(invoiceId)).isInstanceOf(NotFoundException.class);
    }

    private void connectedAs(UUID userId, boolean activeSeller) {
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(userId));
        when(userService.isActiveSeller(userId)).thenReturn(activeSeller);
    }

    private static SaleRDto sale() {
        return new SaleRDto(
                SALE_ID,
                "VTE-0042",
                UUID.randomUUID(),
                DAY,
                UUID.randomUUID(),
                UUID.randomUUID(),
                SELLER,
                PaymentMethod.MVOLA,
                OperationStatus.VALIDATED,
                new BigDecimal("17000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("17000.00"),
                List.of(),
                Instant.parse("2026-11-08T07:00:00Z"),
                SELLER);
    }

    private static InvoiceDraft draft(UUID sellerId) {
        var amount = new BigDecimal("17000.00");
        return new InvoiceDraft(
                SALE_ID,
                "VTE-0042",
                Instant.parse("2026-11-08T07:00:00Z"),
                DAY,
                "Boutique",
                sellerId,
                sellerId == null ? null : "Miora Rakoto",
                "MVOLA",
                "By Valimbavaka Cosmetics",
                null,
                amount,
                new BigDecimal("0.00"),
                amount,
                List.of(new InvoiceLineDraft(UUID.randomUUID(), "Savon coco", new BigDecimal("2.000"), amount, amount)));
    }

    private static InvoiceRDto view(String number) {
        var amount = new BigDecimal("17000.00");
        return new InvoiceRDto(
                UUID.randomUUID(),
                number,
                SALE_ID,
                "VTE-0042",
                Instant.parse("2026-11-08T07:00:00Z"),
                DAY,
                OperationStatus.VALIDATED,
                "Boutique",
                SELLER,
                "Miora Rakoto",
                "MVOLA",
                "By Valimbavaka Cosmetics",
                null,
                amount,
                new BigDecimal("0.00"),
                amount,
                List.of());
    }
}
