package org.vvs.valimbavaka.feature.delivery.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryCustomer;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLineCommand;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleLineCommand;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class DeliverySaleTest {

    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID SOAP_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();

    @Mock
    private SaleService saleService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private UserService userService;

    private DeliverySale sut;

    @BeforeEach
    void setUp() {
        sut = new DeliverySale(saleService, referentialService, userService);
    }

    @Test
    void record_orderEnteredBySeller_recordsADeliverySaleCreditedToHerAtTheOrderPrices() {
        // Arrange
        when(referentialService.findSalesChannelId("DELIVERY")).thenReturn(Optional.of(CHANNEL_ID));
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(true);

        // Act
        sut.record(order(SELLER_ID));

        // Assert
        var command = ArgumentCaptor.forClass(SaleCommand.class);
        verify(saleService).record(command.capture());
        assertThat(command.getValue().salesChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(command.getValue().sellerId()).isEqualTo(SELLER_ID);
        assertThat(command.getValue().paymentMethod()).isEqualTo(PaymentMethod.ON_DELIVERY);
        assertThat(command.getValue().lines())
                .containsExactly(
                        new SaleLineCommand(SOAP_ID, new BigDecimal("2"), new BigDecimal("8500.00")),
                        new SaleLineCommand(OIL_ID, new BigDecimal("0.5"), new BigDecimal("12000.00")));
    }

    @Test
    void record_orderEnteredByAdministrator_recordsASaleWithoutSeller() {
        // Arrange
        var adminId = UUID.randomUUID();
        when(referentialService.findSalesChannelId("DELIVERY")).thenReturn(Optional.of(CHANNEL_ID));
        when(userService.isActiveSeller(adminId)).thenReturn(false);

        // Act
        sut.record(order(adminId));

        // Assert
        var command = ArgumentCaptor.forClass(SaleCommand.class);
        verify(saleService).record(command.capture());
        assertThat(command.getValue().sellerId()).isNull();
    }

    @Test
    void record_orderWithoutAuthor_recordsASaleWithoutSeller() {
        // Arrange
        when(referentialService.findSalesChannelId("DELIVERY")).thenReturn(Optional.of(CHANNEL_ID));

        // Act
        sut.record(order(null));

        // Assert
        var command = ArgumentCaptor.forClass(SaleCommand.class);
        verify(saleService).record(command.capture());
        assertThat(command.getValue().sellerId()).isNull();
        verify(userService, never()).isActiveSeller(any());
    }

    @Test
    void record_deliveryChannelMissing_throwsBusinessExceptionAndRecordsNoSale() {
        // Arrange
        when(referentialService.findSalesChannelId("DELIVERY")).thenReturn(Optional.empty());
        var order = order(SELLER_ID);

        // Act + Assert
        assertThatThrownBy(() -> sut.record(order))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le canal de vente Livraison est introuvable.");
        verify(saleService, never()).record(any());
    }

    @Test
    void record_saleRefused_propagatesTheConflict() {
        // Arrange
        when(referentialService.findSalesChannelId("DELIVERY")).thenReturn(Optional.of(CHANNEL_ID));
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(true);
        when(saleService.record(any())).thenThrow(new ConflictException("Stock insuffisant à Livraison."));
        var order = order(SELLER_ID);

        // Act + Assert
        assertThatThrownBy(() -> sut.record(order)).isInstanceOf(ConflictException.class);
    }

    private static DeliveryOrder order(@Nullable UUID author) {
        var customer = new DeliveryCustomer("Rakoto Be", "034 12 345 67", "Lot II J 45, rue Pasteur", null);
        var lines = List.of(
                new DeliveryOrderLineCommand(SOAP_ID, new BigDecimal("2"), new BigDecimal("8500.00")),
                new DeliveryOrderLineCommand(OIL_ID, new BigDecimal("0.5"), new BigDecimal("12000.00")));
        var command = new DeliveryOrderCommand(customer, UUID.randomUUID(), LocalDate.of(2026, 11, 9), null, lines);
        var order = DeliveryOrder.create("LIV-0042", 1, command);
        ReflectionTestUtils.setField(order, "createdBy", author);
        return order;
    }
}
