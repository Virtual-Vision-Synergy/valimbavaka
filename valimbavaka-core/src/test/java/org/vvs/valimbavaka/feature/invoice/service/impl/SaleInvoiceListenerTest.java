package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.feature.sale.domain.SaleRecordedEvent;

@ExtendWith(MockitoExtension.class)
class SaleInvoiceListenerTest {

    private static final UUID SALE_ID = UUID.randomUUID();

    @Mock
    private InvoiceService invoiceService;

    private SaleInvoiceListener sut;

    @BeforeEach
    void setUp() {
        sut = new SaleInvoiceListener(invoiceService);
    }

    @Test
    void onSaleRecorded_recordedSale_issuesItsInvoice() {
        // Act
        sut.onSaleRecorded(new SaleRecordedEvent(SALE_ID, "VTE-0042"));

        // Assert
        verify(invoiceService).issueForSale(SALE_ID);
    }

    @Test
    void onSaleRecorded_invoiceFails_letsTheErrorRollBackTheSale() {
        // Arrange
        when(invoiceService.issueForSale(SALE_ID)).thenThrow(new NotFoundException("Vente introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.onSaleRecorded(new SaleRecordedEvent(SALE_ID, "VTE-0042")))
                .isInstanceOf(NotFoundException.class);
    }
}
