package org.vvs.valimbavaka.feature.commission.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.commission.domain.Commission;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** No delete exposed (JNL-02): a computed commission is kept; recalculation is COM-05. */
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

    /** Sellers with a non-cancelled commission over {@code [from, to]}, by name; one page of the validation screen. */
    @Query(value = """
            select c.sellerId as sellerId, max(c.sellerName) as sellerName
            from Commission c
            where c.status <> org.vvs.valimbavaka.feature.commission.domain.CommissionStatus.CANCELLED
              and c.businessDate between :from and :to
            group by c.sellerId
            order by max(c.sellerName), c.sellerId
            """, countQuery = """
            select count(distinct c.sellerId) from Commission c
            where c.status <> org.vvs.valimbavaka.feature.commission.domain.CommissionStatus.CANCELLED
              and c.businessDate between :from and :to
            """)
    Page<SellerKey> searchSellers(LocalDate from, LocalDate to, Pageable pageable);

    /** Count and total per seller and status over {@code [from, to]}, cancelled commissions left out. */
    @Query("""
            select c.sellerId as sellerId, c.status as status, count(c) as commissionCount, sum(c.amount) as totalAmount
            from Commission c
            where c.sellerId in :sellerIds
              and c.status <> org.vvs.valimbavaka.feature.commission.domain.CommissionStatus.CANCELLED
              and c.businessDate between :from and :to
            group by c.sellerId, c.status
            """)
    List<SellerStatusTotal> totalsBySellerAndStatus(Collection<UUID> sellerIds, LocalDate from, LocalDate to);

    /** Revenue of the seller's commissioned operations over {@code [from, to]}, cancelled left out; null if none. */
    @Query("""
            select sum(c.revenue) from Commission c
            where c.sellerId = :sellerId
              and c.status <> org.vvs.valimbavaka.feature.commission.domain.CommissionStatus.CANCELLED
              and c.businessDate between :from and :to
            """)
    @Nullable
    BigDecimal sumRevenue(UUID sellerId, LocalDate from, LocalDate to);

    /** Commissions of a seller in one status over {@code [from, to]}. */
    List<Commission> findBySellerIdAndStatusAndBusinessDateBetween(
            UUID sellerId, CommissionStatus status, LocalDate from, LocalDate to);

    /** Whether the seller has a commission, not cancelled, over {@code [from, to]}. */
    boolean existsBySellerIdAndBusinessDateBetweenAndStatusNot(
            UUID sellerId, LocalDate from, LocalDate to, CommissionStatus status);
}
