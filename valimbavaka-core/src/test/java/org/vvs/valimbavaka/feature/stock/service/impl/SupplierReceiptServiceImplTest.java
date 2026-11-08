package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceipt;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceiptCommand;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceiptLineCommand;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptLineWDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptWDto;
import org.vvs.valimbavaka.feature.stock.mapper.SupplierReceiptMapper;
import org.vvs.valimbavaka.feature.stock.repository.SupplierReceiptRepository;
import org.vvs.valimbavaka.feature.stock.service.RestockOrderService;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** STK-06: a supplier receipt puts the goods in stock and is corrected by a reversal, never deleted. */
@ExtendWith(MockitoExtension.class)
class SupplierReceiptServiceImplTest {

    private static final UUID RECEIPT_ID = UUID.randomUUID();
    private static final UUID SUPPLIER_ID = UUID.randomUUID();
    private static final UUID WAREHOUSE_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID SOAP_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();

    @Mock
    private SupplierReceiptRepository supplierReceiptRepository;

    @Mock
    private SupplierReceiptPolicy policy;

    @Mock
    private RestockOrderService restockOrderService;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private JournalService journalService;

    @Captor
    private ArgumentCaptor<List<StockMovementCommand>> movements;

    private SupplierReceiptServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SupplierReceiptServiceImpl(
                supplierReceiptRepository,
                policy,
                restockOrderService,
                stockMovementService,
                new SupplierReceiptMapper(),
                journalService);
    }

    @Test
    void receive_freeEntry_recordsOneReceiptMovementPerLineAndTracesIt() {
        // Arrange
        var request = givenCheckedRequest(null);
        givenSavedReceiptNumber(12);

        // Act
        var result = sut.receive(request);

        // Assert
        assertThat(result.number()).isEqualTo("REC-000012");
        assertThat(result.status()).isEqualTo(OperationStatus.VALIDATED);
        assertThat(result.totalCost()).isEqualByComparingTo("57000.00");
        assertThat(result.lines().get(1).lineCost()).isEqualByComparingTo("21000.00");
        verify(stockMovementService).recordAll(movements.capture());
        assertThat(movements.getValue())
                .containsExactly(
                        new StockMovementCommand(
                                SOAP_ID, WAREHOUSE_ID, StockMovementType.RECEIPT, new BigDecimal("12"), RECEIPT_ID),
                        new StockMovementCommand(
                                OIL_ID, WAREHOUSE_ID, StockMovementType.RECEIPT, new BigDecimal("2.5"), RECEIPT_ID));
        var description =
                "Réception REC-000012 du fournisseur Huiles Ravo (saisie libre) : 2 article(s), 14.5 unité(s)";
        verify(journalService).record(JournalAction.CREATE, "SUPPLIER_RECEIPT", RECEIPT_ID, description);
        verify(restockOrderService, never()).markReceived(any(), any());
    }

    @Test
    void receive_fromRestockOrder_marksTheOrderReceivedAndNamesItInTheJournal() {
        // Arrange
        var request = givenCheckedRequest(ORDER_ID);
        givenSavedReceiptNumber(13);
        when(restockOrderService.markReceived(ORDER_ID, "REC-000013")).thenReturn("BR-000007");

        // Act
        var result = sut.receive(request);

        // Assert
        assertThat(result.restockOrderId()).isEqualTo(ORDER_ID);
        var description =
                "Réception REC-000013 du fournisseur Huiles Ravo (bon BR-000007) : 2 article(s), 14.5 unité(s)";
        verify(journalService).record(JournalAction.CREATE, "SUPPLIER_RECEIPT", RECEIPT_ID, description);
    }

    @Test
    void receive_restockOrderAlreadyReceived_throwsConflictAndSavesNothing() {
        // Arrange
        var request = givenCheckedRequest(ORDER_ID);
        when(supplierReceiptRepository.nextNumber()).thenReturn(14L);
        when(restockOrderService.markReceived(ORDER_ID, "REC-000014")).thenThrow(new ConflictException("reçu"));

        // Act + Assert
        assertThatThrownBy(() -> sut.receive(request)).isInstanceOf(ConflictException.class);
        verify(supplierReceiptRepository, never()).save(any());
        verify(stockMovementService, never()).recordAll(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void reverse_validatedReceipt_recordsInverseMovementsAndTracesTheReason() {
        // Arrange
        var receipt = givenStoredReceipt();
        when(supplierReceiptRepository.save(receipt)).thenReturn(receipt);

        // Act
        var result = sut.reverse(RECEIPT_ID, "  Livraison saisie deux fois ");

        // Assert
        assertThat(result.status()).isEqualTo(OperationStatus.REVERSED);
        verify(stockMovementService).recordAll(movements.capture());
        assertThat(movements.getValue())
                .extracting(StockMovementCommand::quantity)
                .containsExactly(new BigDecimal("-12"), new BigDecimal("-2.5"));
        assertThat(movements.getValue())
                .allSatisfy(movement -> assertThat(movement.type()).isEqualTo(StockMovementType.RECEIPT))
                .allSatisfy(movement -> assertThat(movement.referenceId()).isEqualTo(RECEIPT_ID));
        var description =
                "Contre-passation de la réception %s : Livraison saisie deux fois".formatted(receipt.getNumber());
        verify(journalService).record(JournalAction.REVERSE, "SUPPLIER_RECEIPT", RECEIPT_ID, description);
    }

    @Test
    void reverse_alreadyReversedReceipt_throwsConflictAndMovesNothing() {
        // Arrange
        var receipt = givenStoredReceipt();
        receipt.reverse();

        // Act + Assert
        assertThatThrownBy(() -> sut.reverse(RECEIPT_ID, "Erreur"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("REVERSED");
        verify(stockMovementService, never()).recordAll(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void getReceiptById_unknownId_throwsNotFound() {
        // Arrange
        var id = UUID.randomUUID();
        when(supplierReceiptRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getReceiptById(id)).isInstanceOf(NotFoundException.class);
    }

    private SupplierReceiptWDto givenCheckedRequest(UUID restockOrderId) {
        var request = new SupplierReceiptWDto(
                SUPPLIER_ID,
                null,
                restockOrderId,
                List.of(
                        new SupplierReceiptLineWDto(SOAP_ID, new BigDecimal("12"), new BigDecimal("3000")),
                        new SupplierReceiptLineWDto(OIL_ID, new BigDecimal("2.5"), new BigDecimal("8400"))),
                null);
        when(policy.check(request)).thenReturn(command(restockOrderId));
        return request;
    }

    private void givenSavedReceiptNumber(long number) {
        when(supplierReceiptRepository.nextNumber()).thenReturn(number);
        when(supplierReceiptRepository.save(any(SupplierReceipt.class))).thenAnswer(invocation -> {
            SupplierReceipt receipt = invocation.getArgument(0);
            ReflectionTestUtils.setField(receipt, "id", RECEIPT_ID);
            return receipt;
        });
    }

    private SupplierReceipt givenStoredReceipt() {
        var receipt = SupplierReceipt.create("REC-000003", command(null));
        ReflectionTestUtils.setField(receipt, "id", RECEIPT_ID);
        when(supplierReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        return receipt;
    }

    private static SupplierReceiptCommand command(UUID restockOrderId) {
        return new SupplierReceiptCommand(
                SUPPLIER_ID,
                "Huiles Ravo",
                WAREHOUSE_ID,
                restockOrderId,
                null,
                List.of(
                        new SupplierReceiptLineCommand(
                                SOAP_ID, "ART-0001", "Savon noir 100 g", new BigDecimal("12"), new BigDecimal("3000")),
                        new SupplierReceiptLineCommand(
                                OIL_ID, "ART-0002", "Huile de coco", new BigDecimal("2.5"), new BigDecimal("8400"))));
    }
}
