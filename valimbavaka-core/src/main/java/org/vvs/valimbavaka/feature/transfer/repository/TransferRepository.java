package org.vvs.valimbavaka.feature.transfer.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A recorded transfer is never deleted (JNL-02); gaps are possible in the numbers, a number is never reused. */
public interface TransferRepository extends OperationRepository<Transfer, UUID> {

    @Query(value = "select nextval('transfer_number_seq')", nativeQuery = true)
    long nextTransferNumber();

    @Query(value = "select nextval('stock_return_number_seq')", nativeQuery = true)
    long nextReturnNumber();
}
