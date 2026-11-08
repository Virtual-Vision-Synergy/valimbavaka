package org.vvs.valimbavaka.feature.commission.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.commission.domain.Commission;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** No delete exposed (JNL-02): a computed commission is kept; validation and recalculation are COM-03 and COM-05. */
public interface CommissionRepository extends OperationRepository<Commission, UUID> {

    /** At most one commission per operation (unique constraint on the source). */
    Optional<Commission> findBySourceTypeAndSourceId(CommissionSourceType sourceType, UUID sourceId);

    /** Commissions of the business days {@code [from, to]}, of one seller or of all ({@code sellerId} null). */
    @Query(value = """
            select c from Commission c
            where (:sellerId is null or c.sellerId = :sellerId)
              and c.businessDate between :from and :to
            """, countQuery = """
            select count(c) from Commission c
            where (:sellerId is null or c.sellerId = :sellerId)
              and c.businessDate between :from and :to
            """)
    Page<Commission> search(@Nullable UUID sellerId, LocalDate from, LocalDate to, Pageable pageable);
}
