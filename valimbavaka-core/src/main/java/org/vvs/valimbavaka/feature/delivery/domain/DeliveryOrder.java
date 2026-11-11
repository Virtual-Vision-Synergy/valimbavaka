package org.vvs.valimbavaka.feature.delivery.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Order to deliver (LIV-01): customer, address, assigned driver, day of the round and position in it, priced lines
 * and status. The seller who entered it is the audit author ({@code createdBy}); the driver is referenced by id.
 */
@Entity
@Table(name = "delivery_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryOrder extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 30)
    private String customerPhone;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "district", length = 80)
    private @Nullable String district;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "stop_number", nullable = false)
    private int stopNumber;

    @Column(name = "amount", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "sale_id")
    private @Nullable UUID saleId;

    @Column(name = "note", length = 500)
    private @Nullable String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 30)
    private @Nullable DeliveryFailureReason failureReason;

    @Column(name = "failure_note", length = 280)
    private @Nullable String failureNote;

    @Column(name = "return_transfer_id")
    private @Nullable UUID returnTransferId;

    @ElementCollection
    @CollectionTable(name = "delivery_order_lines", joinColumns = @JoinColumn(name = "delivery_order_id"))
    @OrderColumn(name = "line_number")
    private List<DeliveryOrderLine> lines = new ArrayList<>();

    /**
     * New order, {@link DeliveryStatus#TO_PREPARE}, appended to the round of its driver at {@code stopNumber}; the
     * amount to collect is the sum of the line amounts.
     */
    public static DeliveryOrder create(String number, int stopNumber, DeliveryOrderCommand command) {
        var order = new DeliveryOrder();
        order.number = number;
        order.status = DeliveryStatus.TO_PREPARE;
        order.customerName = command.customer().name();
        order.customerPhone = command.customer().phone();
        order.address = command.customer().address();
        order.district = command.customer().district();
        order.driverId = command.driverId();
        order.scheduledDate = command.scheduledDate();
        order.stopNumber = stopNumber;
        order.note = command.note();
        command.lines().forEach(line -> order.lines.add(DeliveryOrderLine.of(line)));
        order.amount = order.lines.stream().map(DeliveryOrderLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return order;
    }

    /**
     * Delivered (LIV-03): {@link DeliveryStatus#DELIVERED} with the sale of the delivery channel that the order
     * became (decision of the product owner, 07/11/2026); 409 if the order is already delivered, failed or cancelled.
     */
    public void deliver(UUID saleId) {
        moveTo(DeliveryStatus.DELIVERED);
        this.saleId = saleId;
    }

    /** Status change of the following tickets (LIV-02 to LIV-04); 409 if the transition is not allowed. */
    public void moveTo(DeliveryStatus target) {
        status.ensureCanMoveTo(target);
        status = target;
    }

    /**
     * Failure of the delivery (LIV-04): {@link DeliveryStatus#FAILED} with its reason, the driver's comment and the
     * stock return that brought the articles back; 409 if the order is already delivered, failed or cancelled.
     */
    public void fail(DeliveryFailure failure, UUID returnTransferId) {
        moveTo(DeliveryStatus.FAILED);
        this.failureReason = failure.reason();
        this.failureNote = failure.note();
        this.returnTransferId = returnTransferId;
    }

    /** Lines in the order they were entered; they never change once recorded. */
    public List<DeliveryOrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
