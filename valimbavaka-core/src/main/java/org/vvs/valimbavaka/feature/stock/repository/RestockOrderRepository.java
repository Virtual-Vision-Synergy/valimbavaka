package org.vvs.valimbavaka.feature.stock.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.stock.domain.RestockOrder;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** Restock orders (STK-05): no delete, an order is kept for the receipt (STK-06). */
public interface RestockOrderRepository extends OperationRepository<RestockOrder, UUID> {

    /** Number of the next order reference ({@code BR-000001}); a rolled back transaction leaves a gap. */
    @Query(value = "select nextval('restock_order_number_seq')", nativeQuery = true)
    long nextNumber();

    Page<RestockOrder> findAll(Pageable pageable);
}
