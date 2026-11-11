package org.vvs.valimbavaka.feature.delivery.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryCustomer;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLineCommand;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;

@ExtendWith(MockitoExtension.class)
class DeliveryReturnTest {

    private static final UUID DELIVERY_ID = UUID.randomUUID();
    private static final UUID WAREHOUSE_ID = UUID.randomUUID();
    private static final UUID SOAP_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();

    @Mock
    private TransferService transferService;

    @Mock
    private ReferentialService referentialService;

    private DeliveryReturn sut;

    @BeforeEach
    void setUp() {
        sut = new DeliveryReturn(transferService, referentialService);
    }

    @Test
    void record_orderWithTwoLines_returnsEveryArticleFromDeliveryToTheWarehouse() {
        // Arrange
        var returnId = UUID.randomUUID();
        when(referentialService.requireActiveStockLocationId("DELIVERY")).thenReturn(DELIVERY_ID);
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE_ID);
        when(transferService.record(any())).thenReturn(transfer(returnId));

        // Act
        var result = sut.record(order());

        // Assert
        assertThat(result).isEqualTo(returnId);
        var command = ArgumentCaptor.forClass(TransferCommand.class);
        verify(transferService).record(command.capture());
        assertThat(command.getValue().origin()).isEqualTo(TransferOrigin.RETURN);
        assertThat(command.getValue().fromLocationId()).isEqualTo(DELIVERY_ID);
        assertThat(command.getValue().toLocationId()).isEqualTo(WAREHOUSE_ID);
        assertThat(command.getValue().lines())
                .containsExactly(
                        new TransferLineCommand(SOAP_ID, new BigDecimal("2")),
                        new TransferLineCommand(OIL_ID, new BigDecimal("0.5")));
        assertThat(command.getValue().note()).isEqualTo("Retour de la livraison en échec LIV-0042");
    }

    @Test
    void record_deliveryLocationInactive_throwsBusinessExceptionAndRecordsNoReturn() {
        // Arrange
        when(referentialService.requireActiveStockLocationId("DELIVERY"))
                .thenThrow(new BusinessException("L'emplacement de stock DELIVERY est inconnu ou inactif."));
        var order = order();

        // Act + Assert
        assertThatThrownBy(() -> sut.record(order)).isInstanceOf(BusinessException.class);
        verify(transferService, never()).record(any());
    }

    private static DeliveryOrder order() {
        var customer = new DeliveryCustomer("Rakoto Be", "034 12 345 67", "Lot II J 45, rue Pasteur", null);
        var lines = List.of(
                new DeliveryOrderLineCommand(SOAP_ID, new BigDecimal("2"), new BigDecimal("8500.00")),
                new DeliveryOrderLineCommand(OIL_ID, new BigDecimal("0.5"), new BigDecimal("12000.00")));
        var command = new DeliveryOrderCommand(customer, UUID.randomUUID(), LocalDate.of(2026, 11, 9), null, lines);
        return DeliveryOrder.create("LIV-0042", 1, command);
    }

    private static TransferRDto transfer(UUID id) {
        return new TransferRDto(
                id,
                "RET-0007",
                TransferOrigin.RETURN,
                TransferStatus.RETURN,
                DELIVERY_ID,
                WAREHOUSE_ID,
                null,
                List.of(),
                Instant.now(),
                null);
    }
}
