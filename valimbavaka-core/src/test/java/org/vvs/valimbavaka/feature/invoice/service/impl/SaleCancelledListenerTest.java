package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.invoice.service.CreditNoteService;
import org.vvs.valimbavaka.feature.sale.domain.SaleCancelledEvent;

@ExtendWith(MockitoExtension.class)
class SaleCancelledListenerTest {

    private static final UUID SALE_ID = UUID.randomUUID();

    @Mock
    private CreditNoteService creditNoteService;

    private SaleCancelledListener sut;

    @BeforeEach
    void setUp() {
        sut = new SaleCancelledListener(creditNoteService);
    }

    @Test
    void onSaleCancelled_cancelledSale_issuesTheCreditNoteOfItsInvoice() {
        // Act
        sut.onSaleCancelled(new SaleCancelledEvent(SALE_ID, "VTE-0042"));

        // Assert
        verify(creditNoteService).issueForCancelledSale(SALE_ID);
    }

    @Test
    void onSaleCancelled_creditNoteFails_letsTheErrorRollBackTheCancellation() {
        // Arrange
        doThrow(new ConflictException("Facture déjà contre-passée"))
                .when(creditNoteService)
                .issueForCancelledSale(SALE_ID);

        // Act + Assert
        assertThatThrownBy(() -> sut.onSaleCancelled(new SaleCancelledEvent(SALE_ID, "VTE-0042")))
                .isInstanceOf(ConflictException.class);
    }
}
