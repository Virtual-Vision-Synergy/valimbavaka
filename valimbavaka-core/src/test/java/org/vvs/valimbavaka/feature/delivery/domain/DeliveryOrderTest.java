package org.vvs.valimbavaka.feature.delivery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.ConflictException;

class DeliveryOrderTest {

    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 9);
    private static final Duration WINDOW = Duration.ofMinutes(5);

    @Test
    void create_twoLines_startsToPrepareWithTheAmountToCollect() {
        // Arrange
        var command = command(
                new DeliveryOrderLineCommand(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("8500.00")),
                new DeliveryOrderLineCommand(UUID.randomUUID(), new BigDecimal("0.333"), new BigDecimal("1000.00")));

        // Act
        var sut = DeliveryOrder.create("LIV-0007", 3, command);

        // Assert
        assertThat(sut.getNumber()).isEqualTo("LIV-0007");
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        assertThat(sut.getStopNumber()).isEqualTo(3);
        assertThat(sut.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(sut.getScheduledDate()).isEqualTo(DAY);
        assertThat(sut.getCustomerName()).isEqualTo("Rakoto Be");
        assertThat(sut.getDistrict()).isEqualTo("Ivandry");
        assertThat(sut.getLines()).hasSize(2);
        assertThat(sut.getLines().get(1).getAmount()).isEqualByComparingTo("333.00");
        assertThat(sut.getAmount()).isEqualByComparingTo("17333.00");
    }

    @Test
    void getLines_afterCreation_isReadOnly() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));

        // Act + Assert
        assertThatThrownBy(() -> sut.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deliver_orderOnRound_movesToDeliveredWithItsSale() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        sut.moveTo(DeliveryStatus.ON_ROUND);
        var saleId = UUID.randomUUID();

        // Act
        sut.deliver(saleId);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(sut.getSaleId()).isEqualTo(saleId);
    }

    @Test
    void deliver_orderToPrepare_movesToDeliveredInOneStep() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));

        // Act
        sut.deliver(UUID.randomUUID());

        // Assert
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    void deliver_alreadyDeliveredOrder_throwsConflictAndKeepsTheFirstSale() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        var firstSaleId = UUID.randomUUID();
        sut.deliver(firstSaleId);

        // Act + Assert
        assertThatThrownBy(() -> sut.deliver(UUID.randomUUID())).isInstanceOf(ConflictException.class);
        assertThat(sut.getSaleId()).isEqualTo(firstSaleId);
    }

    @Test
    void moveTo_allowedTransition_changesTheStatus() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));

        // Act
        sut.moveTo(DeliveryStatus.ON_ROUND);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.ON_ROUND);
    }

    @Test
    void moveTo_fromFinalStatus_throwsConflictAndKeepsTheStatus() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        sut.moveTo(DeliveryStatus.CANCELLED);

        // Act + Assert
        assertThatThrownBy(() -> sut.moveTo(DeliveryStatus.DELIVERED)).isInstanceOf(ConflictException.class);
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    void fail_orderOnRound_movesToFailedWithItsReasonCommentAndStockReturn() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        sut.moveTo(DeliveryStatus.ON_ROUND);
        var returnTransferId = UUID.randomUUID();

        // Act
        sut.fail(new DeliveryFailure(DeliveryFailureReason.REFUSED, "Colis abîmé"), returnTransferId);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(sut.getFailureReason()).isEqualTo(DeliveryFailureReason.REFUSED);
        assertThat(sut.getFailureNote()).isEqualTo("Colis abîmé");
        assertThat(sut.getReturnTransferId()).isEqualTo(returnTransferId);
    }

    @Test
    void fail_deliveredOrder_throwsConflictAndRecordsNoReason() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        sut.moveTo(DeliveryStatus.DELIVERED);
        var failure = new DeliveryFailure(DeliveryFailureReason.ABSENT, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.fail(failure, UUID.randomUUID())).isInstanceOf(ConflictException.class);
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(sut.getFailureReason()).isNull();
        assertThat(sut.getReturnTransferId()).isNull();
    }

    @Test
    void moveTo_toDelivered_remembersThePreviousStatus() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        sut.moveTo(DeliveryStatus.ON_ROUND);

        // Act
        sut.deliver(UUID.randomUUID());

        // Assert
        assertThat(sut.getPreviousStatus()).isEqualTo(DeliveryStatus.ON_ROUND);
    }

    @Test
    void undo_deliveredOrderWithinTheWindow_restoresThePreviousStatusAndForgetsTheSale() {
        // Arrange
        var sut = delivered(Instant.parse("2026-11-11T08:00:00Z"));

        // Act
        sut.undo(Instant.parse("2026-11-11T08:04:59Z"), WINDOW);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
        assertThat(sut.getPreviousStatus()).isNull();
        assertThat(sut.getSaleId()).isNull();
    }

    @Test
    void undo_failedOrderWithinTheWindow_restoresThePreviousStatusAndForgetsTheFailure() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        sut.moveTo(DeliveryStatus.ON_ROUND);
        sut.fail(new DeliveryFailure(DeliveryFailureReason.ABSENT, "Portail fermé"), UUID.randomUUID());
        ReflectionTestUtils.setField(sut, "updatedAt", Instant.parse("2026-11-11T08:00:00Z"));

        // Act
        sut.undo(Instant.parse("2026-11-11T08:01:00Z"), WINDOW);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.ON_ROUND);
        assertThat(sut.getFailureReason()).isNull();
        assertThat(sut.getFailureNote()).isNull();
        assertThat(sut.getReturnTransferId()).isNull();
    }

    @Test
    void undo_exactlyAtTheEndOfTheWindow_isStillAccepted() {
        // Arrange
        var sut = delivered(Instant.parse("2026-11-11T08:00:00Z"));

        // Act
        sut.undo(Instant.parse("2026-11-11T08:05:00Z"), WINDOW);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
    }

    @Test
    void undo_afterTheWindow_throwsConflictAndKeepsTheDelivery() {
        // Arrange
        var sut = delivered(Instant.parse("2026-11-11T08:00:00Z"));
        var now = Instant.parse("2026-11-11T08:05:01Z");

        // Act + Assert
        assertThatThrownBy(() -> sut.undo(now, WINDOW))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("5 minute(s)");
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(sut.getSaleId()).isNotNull();
    }

    @Test
    void undo_orderNeverDeliveredNorFailed_throwsConflict() {
        // Arrange
        var sut = DeliveryOrder.create("LIV-0001", 1, command(line()));
        ReflectionTestUtils.setField(sut, "updatedAt", Instant.parse("2026-11-11T08:00:00Z"));
        var now = Instant.parse("2026-11-11T08:01:00Z");

        // Act + Assert
        assertThatThrownBy(() -> sut.undo(now, WINDOW)).isInstanceOf(ConflictException.class);
        assertThat(sut.getStatus()).isEqualTo(DeliveryStatus.TO_PREPARE);
    }

    @Test
    void undo_alreadyUndone_throwsConflict() {
        // Arrange
        var sut = delivered(Instant.parse("2026-11-11T08:00:00Z"));
        var now = Instant.parse("2026-11-11T08:01:00Z");
        sut.undo(now, WINDOW);

        // Act + Assert
        assertThatThrownBy(() -> sut.undo(now, WINDOW)).isInstanceOf(ConflictException.class);
    }

    private static DeliveryOrder delivered(Instant updatedAt) {
        var order = DeliveryOrder.create("LIV-0001", 1, command(line()));
        order.deliver(UUID.randomUUID());
        ReflectionTestUtils.setField(order, "updatedAt", updatedAt);
        return order;
    }

    private static DeliveryOrderLineCommand line() {
        return new DeliveryOrderLineCommand(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("8500.00"));
    }

    private static DeliveryOrderCommand command(DeliveryOrderLineCommand... lines) {
        var customer = new DeliveryCustomer("Rakoto Be", "034 12 345 67", "Lot II J 45, rue Pasteur", "Ivandry");
        return new DeliveryOrderCommand(customer, DRIVER_ID, DAY, null, List.of(lines));
    }
}
