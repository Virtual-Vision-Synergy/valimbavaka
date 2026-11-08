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
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryFailureReason;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLineCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryFailureWDto;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class DeliveryFailureServiceImplTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID RETURN_ID = UUID.randomUUID();

    @Mock
    private DeliveryOrderRepository orderRepository;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliveryReturn deliveryReturn;

    @Mock
    private JournalService journalService;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private DeliveryFailureServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DeliveryFailureServiceImpl(
                orderRepository, deliveryMapper, deliveryReturn, journalService, userService, auditorAware);
    }

    @Test
    void reportFailure_ownOrder_failsTheOrderReturnsTheArticlesAndTracesIt() {
        // Arrange
        var order = order();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(deliveryReturn.record(order)).thenReturn(RETURN_ID);

        // Act
        sut.reportFailure(ORDER_ID, new DeliveryFailureWDto(DeliveryFailureReason.ABSENT, "  Portail fermé  "));

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo(DeliveryFailureReason.ABSENT);
        assertThat(order.getFailureNote()).isEqualTo("Portail fermé");
        assertThat(order.getReturnTransferId()).isEqualTo(RETURN_ID);
        verify(journalService)
                .record(
                        eq(JournalAction.UPDATE),
                        eq("DELIVERY_ORDER"),
                        isNull(),
                        eq("Commande LIV-0042 en échec : client absent, 1 article(s) remis en stock"));
    }

    @Test
    void reportFailure_blankComment_storesNoComment() {
        // Arrange
        var order = order();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(deliveryReturn.record(order)).thenReturn(RETURN_ID);

        // Act
        sut.reportFailure(ORDER_ID, new DeliveryFailureWDto(DeliveryFailureReason.OTHER, "   "));

        // Assert
        assertThat(order.getFailureNote()).isNull();
    }

    @Test
    void reportFailure_administrator_failsAnOrderOfAnyDriver() {
        // Arrange
        var order = order();
        var adminId = UUID.randomUUID();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(adminId));
        when(userService.isActiveDriver(adminId)).thenReturn(false);
        when(deliveryReturn.record(order)).thenReturn(RETURN_ID);

        // Act
        sut.reportFailure(ORDER_ID, new DeliveryFailureWDto(DeliveryFailureReason.REFUSED, null));

        // Assert
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void reportFailure_orderOfAnotherDriver_throwsNotFoundAndWritesNothing() {
        // Arrange
        var order = order();
        var otherDriverId = UUID.randomUUID();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(otherDriverId));
        when(userService.isActiveDriver(otherDriverId)).thenReturn(true);
        var request = new DeliveryFailureWDto(DeliveryFailureReason.ABSENT, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.reportFailure(ORDER_ID, request)).isInstanceOf(NotFoundException.class);
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        verify(deliveryReturn, never()).record(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void reportFailure_unknownOrder_throwsNotFound() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());
        var request = new DeliveryFailureWDto(DeliveryFailureReason.ABSENT, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.reportFailure(ORDER_ID, request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void reportFailure_deliveredOrder_throwsConflictBeforeAnyStockReturn() {
        // Arrange
        var order = order();
        order.moveTo(DeliveryStatus.DELIVERED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        var request = new DeliveryFailureWDto(DeliveryFailureReason.ABSENT, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.reportFailure(ORDER_ID, request)).isInstanceOf(ConflictException.class);
        verify(deliveryReturn, never()).record(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void reportFailure_stockReturnRefused_leavesTheOrderUnchangedAndTracesNothing() {
        // Arrange
        var order = order();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(deliveryReturn.record(order)).thenThrow(new ConflictException("Stock insuffisant à Livraison."));
        var request = new DeliveryFailureWDto(DeliveryFailureReason.ABSENT, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.reportFailure(ORDER_ID, request)).isInstanceOf(ConflictException.class);
        assertThat(order.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    private static DeliveryOrder order() {
        var customer = new DeliveryCustomer("Rakoto Be", "034 12 345 67", "Lot II J 45, rue Pasteur", "Ivandry");
        var line = new DeliveryOrderLineCommand(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("8500.00"));
        var command = new DeliveryOrderCommand(customer, DRIVER_ID, LocalDate.of(2026, 11, 9), null, List.of(line));
        return DeliveryOrder.create("LIV-0042", 1, command);
    }
}
