package org.vvs.valimbavaka.feature.transfer.repository;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A recorded transfer is never deleted (JNL-02); gaps are possible in the numbers, a number is never reused. */
public interface TransferRepository extends OperationRepository<Transfer, UUID> {

    @Query(value = "select nextval('transfer_number_seq')", nativeQuery = true)
    long nextTransferNumber();

    @Query(value = "select nextval('stock_return_number_seq')", nativeQuery = true)
    long nextReturnNumber();

    /**
     * Transfer history (TRF-01), one row per transfer with the count and total quantity of its lines, in a single
     * query. Case-insensitive search on the number; empty {@code search} = all, null {@code origin} = all.
     */
    @Query(value = """
            select new org.vvs.valimbavaka.feature.transfer.repository.TransferSummary(
                t.id, t.number, t.origin, t.status, t.fromLocationId, t.toLocationId,
                count(l.articleId), sum(l.quantity), t.createdAt, t.createdBy)
            from Transfer t join t.lines l
            where lower(t.number) like lower(concat('%', :search, '%'))
              and (:origin is null or t.origin = :origin)
            group by t.id
            """, countQuery = """
            select count(t) from Transfer t
            where lower(t.number) like lower(concat('%', :search, '%'))
              and (:origin is null or t.origin = :origin)
            """)
    Page<TransferSummary> search(String search, @Nullable TransferOrigin origin, Pageable pageable);
}
