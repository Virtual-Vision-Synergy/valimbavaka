package org.vvs.valimbavaka.feature.delivery.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryCustomer;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderFilter;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLineCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryEventRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderLineWDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderWDto;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.dto.JournalEntryRDto;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceImplTest {

    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID OTHER_DRIVER_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 9);

    @Mock
    private DeliveryOrderRepository orderRepository;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliveryOrderPolicy policy;

    @Mock
    private JournalService journalService;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private DeliveryOrderServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DeliveryOrderServiceImpl(
                orderRepository, deliveryMapper, policy, journalService, userService, auditorAware);
    }

    @Test
    void createOrder_validRequest_appendsTheOrderToTheRoundOfItsDriverAndTracesIt() {
        // Arrange
        when(policy.check(any())).thenReturn(command());
        when(orderRepository.nextStopNumber(DRIVER_ID, DAY)).thenReturn(3);
        when(orderRepository.nextNumber()).thenReturn(42L);
        when(orderRepository.save(any(DeliveryOrder.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.createOrder(request());

        // Assert
        var saved = ArgumentCaptor.forClass(DeliveryOrder.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getNumber()).isEqualTo("LIV-0042");
        assertThat(saved.getValue().getStopNumber()).isEqualTo(3);
        assertThat(saved.getValue().getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        var description = ArgumentCaptor.forClass(String.class);
        verify(journalService).record(eq(JournalAction.CREATE), eq("DELIVERY_ORDER"), isNull(), description.capture());
        assertThat(description.getValue())
                .startsWith("Création de la commande à livrer LIV-0042 : 1 article(s), 17000.00 Ar")
                .endsWith("tournée du 2026-11-09 (arrêt 3)");
    }

    @Test
    void createOrder_validRequest_locksTheRoundBeforeReadingTheNextStop() {
        // Arrange
        when(policy.check(any())).thenReturn(command());
        when(orderRepository.save(any(DeliveryOrder.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.createOrder(request());

        // Assert
        var order = inOrder(orderRepository);
        order.verify(orderRepository).lockRound("delivery-round:" + DRIVER_ID + ":2026-11-09");
        order.verify(orderRepository).nextStopNumber(DRIVER_ID, DAY);
    }

    @Test
    void createOrder_rejectedByThePolicy_writesNothing() {
        // Arrange
        var refusal = new BusinessException("La date de livraison ne peut pas être dans le passé.");
        when(policy.check(any())).thenThrow(refusal);

        // Act + Assert
        assertThatThrownBy(() -> sut.createOrder(request())).isInstanceOf(BusinessException.class);
        verify(orderRepository, never()).save(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void getOrder_unknownId_throwsNotFound() {
        // Arrange
        var orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getOrder(orderId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOrder_driverReadingAnotherDriversOrder_throwsNotFound() {
        // Arrange
        var orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order()));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(OTHER_DRIVER_ID));
        when(userService.isActiveDriver(OTHER_DRIVER_ID)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.getOrder(orderId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOrder_sellerReadingAnyOrder_returnsIt() {
        // Arrange
        var orderId = UUID.randomUUID();
        var order = order();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(userService.isActiveDriver(SELLER_ID)).thenReturn(false);

        // Act
        sut.getOrder(orderId);

        // Assert
        verify(deliveryMapper).toRDto(order);
    }

    @Test
    void getOrders_driverAskingForAnotherDriver_isLimitedToHisOwnOrders() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(DRIVER_ID));
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);
        when(orderRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        // Act
        sut.getOrders(new PageQuery(1, 20, null, null, null), new DeliveryOrderFilter(OTHER_DRIVER_ID, DAY, null));

        // Assert
        verify(orderRepository).search(eq(DRIVER_ID), eq(DAY), isNull(), any(Pageable.class));
    }

    @Test
    void getOrders_administratorWithFilters_passesThemUnchanged() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(userService.isActiveDriver(SELLER_ID)).thenReturn(false);
        when(orderRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        var status = DeliveryStatus.TO_PREPARE;
        var filter = new DeliveryOrderFilter(OTHER_DRIVER_ID, DAY, status);

        // Act
        sut.getOrders(new PageQuery(1, 20, null, "stopNumber", null), filter);

        // Assert
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).search(eq(OTHER_DRIVER_ID), eq(DAY), eq(status), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("stopNumber")).isNotNull();
    }

    @Test
    void getHistory_visibleOrder_returnsItsJournalLinesAsSteps() {
        // Arrange
        var orderId = UUID.randomUUID();
        var query = new PageQuery(1, 20, null, null, null);
        var entry = new JournalEntryRDto(
                UUID.randomUUID(),
                Instant.parse("2026-11-08T06:15:00Z"),
                SELLER_ID,
                "Hanta Rabe",
                JournalAction.CREATE,
                DeliveryOrderServiceImpl.RESOURCE_TYPE,
                orderId,
                "Création de la commande à livrer LIV-0001");
        var step = new DeliveryEventRDto(entry.occurredAt(), "Hanta Rabe", "CREATE", entry.description());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order()));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(userService.isActiveDriver(SELLER_ID)).thenReturn(false);
        when(journalService.getResourceEntries(DeliveryOrderServiceImpl.RESOURCE_TYPE, orderId, query))
                .thenReturn(new PageResponse<>(List.of(entry), 1, 20, 1, 1));
        when(deliveryMapper.toEventRDto(entry)).thenReturn(step);

        // Act
        var result = sut.getHistory(orderId, query);

        // Assert
        assertThat(result.items()).containsExactly(step);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void getHistory_driverReadingAnotherDriversOrder_throwsNotFoundWithoutReadingTheJournal() {
        // Arrange
        var orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order()));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(OTHER_DRIVER_ID));
        when(userService.isActiveDriver(OTHER_DRIVER_ID)).thenReturn(true);
        var query = new PageQuery(1, 20, null, null, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.getHistory(orderId, query)).isInstanceOf(NotFoundException.class);
        verify(journalService, never()).getResourceEntries(any(), any(), any());
    }

    private static DeliveryOrderWDto request() {
        return new DeliveryOrderWDto(
                "Rakoto Be",
                "034 12 345 67",
                "Lot II J 45, rue Pasteur",
                null,
                DRIVER_ID,
                DAY,
                List.of(new DeliveryOrderLineWDto(UUID.randomUUID(), new BigDecimal("2"))),
                null);
    }

    private static DeliveryOrderCommand command() {
        var customer = new DeliveryCustomer("Rakoto Be", "034 12 345 67", "Lot II J 45, rue Pasteur", null);
        var line = new DeliveryOrderLineCommand(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("8500.00"));
        return new DeliveryOrderCommand(customer, DRIVER_ID, DAY, null, List.of(line));
    }

    private static DeliveryOrder order() {
        return DeliveryOrder.create("LIV-0001", 1, command());
    }
}
