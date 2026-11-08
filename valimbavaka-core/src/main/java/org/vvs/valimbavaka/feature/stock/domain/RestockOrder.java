package org.vvs.valimbavaka.feature.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Header of a restock order (STK-05): reference given by the server, status. Its lines are {@link RestockOrderLine}
 * rows referencing it by id. Never deleted: the repository exposes no delete (JNL-02). Received by STK-06.
 */
@Entity
@Table(name = "restock_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestockOrder extends BaseEntity {

    @Column(name = "reference", nullable = false, updatable = false, length = 20)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RestockOrderStatus status;

    /** @param number next value of the {@code restock_order_number_seq} sequence */
    public static RestockOrder create(long number) {
        var order = new RestockOrder();
        order.reference = "BR-%06d".formatted(number);
        order.status = RestockOrderStatus.PENDING;
        return order;
    }

    /**
     * Receipt of the delivery (STK-06): {@code PENDING} to {@code RECEIVED}. An order is taken back by one receipt.
     *
     * @throws ConflictException if the order is already received
     */
    public void markReceived() {
        if (status == RestockOrderStatus.RECEIVED) {
            throw new ConflictException("Le bon de réapprovisionnement %s est déjà réceptionné.".formatted(reference));
        }
        this.status = RestockOrderStatus.RECEIVED;
    }
}
