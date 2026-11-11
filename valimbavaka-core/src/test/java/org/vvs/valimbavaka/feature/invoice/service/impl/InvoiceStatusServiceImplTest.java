package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceStatusTotalRDto;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceRepository;
import org.vvs.valimbavaka.feature.invoice.repository.InvoiceStatusTotal;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

class InvoiceStatusServiceImplTest {

    private static final LocalDate FIRST = LocalDate.of(2026, 11, 1);
    private static final LocalDate LAST = LocalDate.of(2026, 11, 30);

    private InvoiceRepository invoiceRepository;
    private InvoiceStatusServiceImpl sut;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        // 2026-11-10 21:30 UTC is already 2026-11-11 in Antananarivo (UTC+3)
        var clock = Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
        sut = new InvoiceStatusServiceImpl(invoiceRepository, clock);
    }

    @Test
    void getStatusTotals_someStatusesPresent_listsEveryStatusAndTheGrandTotalIsTheirSum() {
        // Arrange
        when(invoiceRepository.totalsByStatus(FIRST, LAST))
                .thenReturn(List.of(
                        new InvoiceStatusTotal(OperationStatus.REVERSED, 1, new BigDecimal("4000.00")),
                        new InvoiceStatusTotal(OperationStatus.VALIDATED, 2, new BigDecimal("3500.00"))));

        // Act
        var result = sut.getStatusTotals(FIRST, LAST);

        // Assert
        assertThat(result.statuses())
                .extracting(InvoiceStatusTotalRDto::status)
                .containsExactly(OperationStatus.VALIDATED, OperationStatus.CANCELLED, OperationStatus.REVERSED);
        assertThat(result.statuses().get(1).invoiceCount()).isZero();
        assertThat(result.statuses().get(1).amount()).isEqualByComparingTo("0");
        assertThat(result.invoiceCount()).isEqualTo(3);
        assertThat(result.amount()).isEqualByComparingTo("7500.00");
    }

    @Test
    void getStatusTotals_noInvoice_returnsZeroEverywhere() {
        // Arrange
        when(invoiceRepository.totalsByStatus(FIRST, LAST)).thenReturn(List.of());

        // Act
        var result = sut.getStatusTotals(FIRST, LAST);

        // Assert
        assertThat(result.statuses()).hasSize(3).allSatisfy(total -> assertThat(total.invoiceCount()).isZero());
        assertThat(result.amount()).isEqualByComparingTo("0");
    }

    @Test
    void getStatusTotals_noDates_usesTheMonthToDateOfTheShopDay() {
        // Arrange
        when(invoiceRepository.totalsByStatus(any(), any())).thenReturn(List.of());

        // Act
        var result = sut.getStatusTotals(null, null);

        // Assert
        assertThat(result.from()).isEqualTo(FIRST);
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 11, 11));
        verify(invoiceRepository).totalsByStatus(FIRST, LocalDate.of(2026, 11, 11));
    }

    @Test
    void getStatusTotals_fromAfterTo_throwsBusinessExceptionWithoutReading() {
        // Act + Assert
        assertThatThrownBy(() -> sut.getStatusTotals(LAST, FIRST)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(invoiceRepository);
    }
}
