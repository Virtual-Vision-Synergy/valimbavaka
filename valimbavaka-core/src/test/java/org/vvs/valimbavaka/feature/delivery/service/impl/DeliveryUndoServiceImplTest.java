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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryCustomer;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryFailure;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryFailureReason;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLineCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class DeliveryUndoServiceImplTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID SALE_ID = UUID.randomUUID();
    private static final Instant DONE_AT = Instant.parse("2026-11-11T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-11T08:03:00Z"), ZoneOffset.UTC);

    @Mock
    private DeliveryOrderRepository orderRepository;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliveryReturn deliveryReturn;

    @Mock
    private SaleService saleService;

    @Mock
    private JournalService journalService;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private DeliveryUndoServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DeliveryUndoServiceImpl(
                orderRepository,
                deliveryMapper,
                deliveryReturn,
                saleService,
                journalService,
                userService,
                auditorAware,
                CLOCK);
    }

    @Test
    void undoLastAction_deliveredOrder_cancelsTheSaleRestoresTheStatusAndTracesIt() {
        // Arrange
        var order = delivered();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));

        // Act
        sut.undoLastAction(ORDER_ID);

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        assertThat(order.getSaleId()).isNull();
        verify(saleService).cancel(SALE_ID, "annulation de la livraison LIV-0042");
        verify(deliveryReturn, never()).undoReturn(any());
        verify(journalService)
                .record(
                        eq(JournalAction.CANCEL),
                        eq("DELIVERY_ORDER"),
                        isNull(),
                        eq("Annulation de l'action sur la commande LIV-0042 : de « livrée » à « à préparer »"));
    }

    @Test
    void undoLastAction_failedOrder_cancelsTheReturnAndRestoresTheStatus() {
        // Arrange
        var order = failed();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));

        // Act
        sut.undoLastAction(ORDER_ID);

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        assertThat(order.getReturnTransferId()).isNull();
        verify(deliveryReturn).undoReturn(order);
        verify(saleService, never()).cancel(any(), any());
    }

    @Test
    void undoLastAction_delayOver_throwsConflictBeforeTouchingTheSaleOrTheStock() {
        // Arrange
        var order = delivered();
        ReflectionTestUtils.setField(order, "updatedAt", Instant.parse("2026-11-11T07:50:00Z"));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.undoLastAction(ORDER_ID)).isInstanceOf(ConflictException.class);
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(saleService, never()).cancel(any(), any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void undoLastAction_orderToPrepare_throwsConflict() {
        // Arrange
        var order = order();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.undoLastAction(ORDER_ID)).isInstanceOf(ConflictException.class);
        verify(saleService, never()).cancel(any(), any());
    }

    @Test
    void undoLastAction_saleCannotBeCancelled_keepsTheOrderDelivered() {
        // Arrange
        var order = delivered();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(saleService.cancel(any(), any())).thenThrow(new ConflictException("Journée clôturée."));

        // Act + Assert
        assertThatThrownBy(() -> sut.undoLastAction(ORDER_ID)).isInstanceOf(ConflictException.class);
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(order.getSaleId()).isEqualTo(SALE_ID);
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void undoLastAction_administrator_undoesAnOrderOfAnyDriver() {
        // Arrange
        var order = delivered();
        var adminId = UUID.randomUUID();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(adminId));
        when(userService.isActiveDriver(adminId)).thenReturn(false);

        // Act
        sut.undoLastAction(ORDER_ID);

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
    }

    @Test
    void undoLastAction_orderOfAnotherDriver_throwsNotFoundAndCancelsNothing() {
        // Arrange
        var order = delivered();
        var otherDriverId = UUID.randomUUID();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(otherDriverId));
        when(userService.isActiveDriver(otherDriverId)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.undoLastAction(ORDER_ID)).isInstanceOf(NotFoundException.class);
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(saleService, never()).cancel(any(), any());
    }

    @Test
    void undoLastAction_unknownOrder_throwsNotFound() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.undoLastAction(ORDER_ID)).isInstanceOf(NotFoundException.class);
    }

    private static DeliveryOrder delivered() {
        var order = order();
        order.deliver(SALE_ID);
        ReflectionTestUtils.setField(order, "updatedAt", DONE_AT);
        return order;
    }

    private static DeliveryOrder failed() {
        var order = order();
        order.fail(new DeliveryFailure(DeliveryFailureReason.ABSENT, null), UUID.randomUUID());
        ReflectionTestUtils.setField(order, "updatedAt", DONE_AT);
        return order;
    }

    private static DeliveryOrder order() {
        var customer = new DeliveryCustomer("Rakoto Be", "034 12 345 67", "Lot II J 45, rue Pasteur", "Ivandry");
        var line = new DeliveryOrderLineCommand(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("8500.00"));
        var command = new DeliveryOrderCommand(customer, DRIVER_ID, LocalDate.of(2026, 11, 9), null, List.of(line));
        var order = DeliveryOrder.create("LIV-0042", 1, command);
        ReflectionTestUtils.setField(order, "updatedAt", DONE_AT);
        return order;
    }
}
