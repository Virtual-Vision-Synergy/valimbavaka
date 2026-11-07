package org.vvs.valimbavaka.feature.stock.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.stock.domain.SupplierReceipt;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** Supplier receipts (STK-06): no delete, an error is corrected by a reversal (JNL-02). */
public interface SupplierReceiptRepository extends OperationRepository<SupplierReceipt, UUID> {

    /** Number of the next receipt ({@code REC-000001}); a rolled back transaction leaves a gap. */
    @Query(value = "select nextval('supplier_receipt_number_seq')", nativeQuery = true)
    long nextNumber();

    Page<SupplierReceipt> findAll(Pageable pageable);
}
