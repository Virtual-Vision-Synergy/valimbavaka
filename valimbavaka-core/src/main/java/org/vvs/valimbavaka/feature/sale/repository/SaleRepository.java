package org.vvs.valimbavaka.feature.sale.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A recorded sale is never deleted (JNL-02); gaps are possible in the numbers, a number is never reused. */
public interface SaleRepository extends OperationRepository<Sale, UUID> {

    @Query(value = "select nextval('sale_number_seq')", nativeQuery = true)
    long nextNumber();

    /** Number the next sale will probably get, without consuming it (two counters may show the same one). */
    @Query(
            value = "select case when is_called then last_value + 1 else last_value end from sale_number_seq",
            nativeQuery = true)
    long peekNextNumber();
}
