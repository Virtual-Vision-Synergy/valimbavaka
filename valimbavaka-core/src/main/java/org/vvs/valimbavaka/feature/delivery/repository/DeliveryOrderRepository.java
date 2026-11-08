package org.vvs.valimbavaka.feature.delivery.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** An order to deliver is never deleted (JNL-02): it is cancelled or fails; a number is never reused. */
public interface DeliveryOrderRepository extends OperationRepository<DeliveryOrder, UUID> {

    @Query(value = "select nextval('delivery_order_number_seq')", nativeQuery = true)
    long nextNumber();

    /** Serialises the creations on the same round (driver + day) until the end of the transaction. */
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(:key, 0)) is not null", nativeQuery = true)
    boolean lockRound(String key);

    /** Next position in the round of the driver for the day: 1 for an empty round, last position + 1 otherwise. */
    @Query("""
            select coalesce(max(d.stopNumber), 0) + 1 from DeliveryOrder d
            where d.driverId = :driverId and d.scheduledDate = :scheduledDate
            """)
    int nextStopNumber(UUID driverId, LocalDate scheduledDate);

    /** Orders matching the filters; a {@code null} filter = all (driver + day = the round of a driver). */
    @Query("""
            select d from DeliveryOrder d
            where (:driverId is null or d.driverId = :driverId)
              and d.scheduledDate = coalesce(:scheduledDate, d.scheduledDate)
              and (:status is null or d.status = :status)
            """)
    Page<DeliveryOrder> search(
            @Nullable UUID driverId,
            @Nullable LocalDate scheduledDate,
            @Nullable DeliveryStatus status,
            Pageable pageable);
}
