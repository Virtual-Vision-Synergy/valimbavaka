package org.vvs.valimbavaka.feature.delivery.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryCustomer;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLineCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class DeliveryCompletionServiceImplTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID SALE_ID = UUID.randomUUID();

    @Mock
    private DeliveryOrderRepository orderRepository;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliverySale deliverySale;

    @Mock
    private JournalService journalService;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private DeliveryCompletionServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DeliveryCompletionServiceImpl(
                orderRepository, deliveryMapper, deliverySale, journalService, userService, auditorAware);
    }

    @Test
    void markDelivered_ownOrder_recordsTheSaleDeliversTheOrderAndTracesIt() {
        // Arrange
        var order = order();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(deliverySale.record(order)).thenReturn(sale());

        // Act
        sut.markDelivered(ORDER_ID);

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(order.getSaleId()).isEqualTo(SALE_ID);
        verify(journalService)
                .record(
                        eq(JournalAction.UPDATE),
                        eq("DELIVERY_ORDER"),
                        isNull(),
                        eq("Commande LIV-0042 livrée : vente VTE-0007, 1 article(s), 17000.00 Ar à la livraison"));
    }

    @Test
    void markDelivered_orderOnRound_deliversItToo() {
        // Arrange
        var order = order();
        order.moveTo(DeliveryStatus.ON_ROUND);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(deliverySale.record(order)).thenReturn(sale());

        // Act
        sut.markDelivered(ORDER_ID);

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    void markDelivered_administrator_deliversAnOrderOfAnyDriver() {
        // Arrange
        var order = order();
        var adminId = UUID.randomUUID();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(adminId));
        when(userService.isActiveDriver(adminId)).thenReturn(false);
        when(deliverySale.record(order)).thenReturn(sale());

        // Act
        sut.markDelivered(ORDER_ID);

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    void markDelivered_orderOfAnotherDriver_throwsNotFoundAndRecordsNoSale() {
        // Arrange
        var order = order();
        var otherDriverId = UUID.randomUUID();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(otherDriverId));
        when(userService.isActiveDriver(otherDriverId)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.markDelivered(ORDER_ID)).isInstanceOf(NotFoundException.class);
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        verify(deliverySale, never()).record(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void markDelivered_unknownOrder_throwsNotFound() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.markDelivered(ORDER_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void markDelivered_alreadyDelivered_throwsConflictBeforeAnySale() {
        // Arrange
        var order = order();
        order.deliver(UUID.randomUUID());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.markDelivered(ORDER_ID)).isInstanceOf(ConflictException.class);
        verify(deliverySale, never()).record(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void markDelivered_saleRefused_leavesTheOrderUnchangedAndTracesNothing() {
        // Arrange
        var order = order();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(deliverySale.record(order)).thenThrow(new ConflictException("La journée n'est pas ouverte."));

        // Act + Assert
        assertThatThrownBy(() -> sut.markDelivered(ORDER_ID)).isInstanceOf(ConflictException.class);
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        assertThat(order.getSaleId()).isNull();
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    private static DeliveryOrder order() {
        var customer = new DeliveryCustomer("Rakoto Be", "034 12 345 67", "Lot II J 45, rue Pasteur", "Ivandry");
        var line = new DeliveryOrderLineCommand(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("8500.00"));
        var command = new DeliveryOrderCommand(customer, DRIVER_ID, LocalDate.of(2026, 11, 9), null, List.of(line));
        return DeliveryOrder.create("LIV-0042", 1, command);
    }

    private static SaleRDto sale() {
        var total = new BigDecimal("17000.00");
        return new SaleRDto(
                SALE_ID,
                "VTE-0007",
                UUID.randomUUID(),
                LocalDate.of(2026, 11, 9),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                PaymentMethod.ON_DELIVERY,
                OperationStatus.VALIDATED,
                total,
                BigDecimal.ZERO,
                total,
                List.of(),
                Instant.now(),
                null);
    }
}
