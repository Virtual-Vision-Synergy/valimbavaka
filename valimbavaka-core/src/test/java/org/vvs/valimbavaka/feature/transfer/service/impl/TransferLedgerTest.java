package org.vvs.valimbavaka.feature.transfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;

@ExtendWith(MockitoExtension.class)
class TransferLedgerTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID WAREHOUSE = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private JournalService journalService;

    @Captor
    private ArgumentCaptor<List<StockMovementCommand>> movements;

    private TransferLedger sut;

    @BeforeEach
    void setUp() {
        sut = new TransferLedger(stockMovementService, journalService);
    }

    @Test
    void post_twoLines_recordsAnOutAndAnInTransferMovementPerLineThenTracesTheTransfer() {
        // Arrange
        var soap = new TransferLineCommand(SOAP, new BigDecimal("4"));
        var transfer = returnOf(soap, new TransferLineCommand(OIL, new BigDecimal("1.5")));

        // Act
        sut.post(transfer, "Boutique → Entrepôt");

        // Assert
        verify(stockMovementService).recordAll(movements.capture());
        assertThat(movements.getValue())
                .extracting(
                        StockMovementCommand::articleId,
                        StockMovementCommand::locationId,
                        StockMovementCommand::type,
                        movement -> movement.quantity().toPlainString())
                .containsExactly(
                        tuple(SOAP, STORE, StockMovementType.TRANSFER, "-4"),
                        tuple(SOAP, WAREHOUSE, StockMovementType.TRANSFER, "4"),
                        tuple(OIL, STORE, StockMovementType.TRANSFER, "-1.5"),
                        tuple(OIL, WAREHOUSE, StockMovementType.TRANSFER, "1.5"));
        verify(journalService)
                .record(
                        JournalAction.CREATE,
                        "TRANSFER",
                        transfer.getId(),
                        "Retour de stock RET-0001 · Boutique → Entrepôt : 2 article(s), 5.5 unité(s)");
    }

    @Test
    void post_stockRefused_tracesNothing() {
        // Arrange
        var transfer = returnOf(new TransferLineCommand(SOAP, BigDecimal.TEN));
        when(stockMovementService.recordAll(anyList())).thenThrow(new ConflictException("Stock insuffisant"));

        // Act + Assert
        assertThatThrownBy(() -> sut.post(transfer, "Boutique → Entrepôt")).isInstanceOf(ConflictException.class);
        verifyNoInteractions(journalService);
    }

    private static Transfer returnOf(TransferLineCommand... lines) {
        return Transfer.create(
                "RET-0001", new TransferCommand(TransferOrigin.RETURN, STORE, WAREHOUSE, List.of(lines), null));
    }
}
