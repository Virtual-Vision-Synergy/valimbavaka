package org.vvs.valimbavaka.feature.production.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A batch is never deleted (JNL-02): no delete method, no {@code JpaRepository}. */
public interface ProductionBatchRepository extends OperationRepository<ProductionBatch, UUID> {

    /** Next number of the batch sequence (gaps are possible, a number is never reused). */
    @Query(value = "select nextval('production_batch_number_seq')", nativeQuery = true)
    long nextNumber();

    /** Case-insensitive search on the number; empty {@code search} = all, null status = all. */
    @Query(value = """
            select b from ProductionBatch b
            where lower(b.number) like lower(concat('%', :search, '%'))
              and (:status is null or b.status = :status)
            """, countQuery = """
            select count(b) from ProductionBatch b
            where lower(b.number) like lower(concat('%', :search, '%'))
              and (:status is null or b.status = :status)
            """)
    Page<ProductionBatch> search(String search, @Nullable ProductionBatchStatus status, Pageable pageable);

    /** Batch locked in write until the end of the transaction: two completions of the same batch are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ProductionBatch b where b.id = :id")
    Optional<ProductionBatch> findByIdForUpdate(UUID id);

    /** Saves and flushes, so that the returned version and dates are those of the database (PRD-02). */
    ProductionBatch saveAndFlush(ProductionBatch batch);

    /** Batches completed in [from, to) (PRD-04): {@code completed_at} is set only on COMPLETED batches (PRD-02). */
    @Query("select b from ProductionBatch b where b.completedAt >= :from and b.completedAt < :to")
    List<ProductionBatch> findCompletedBetween(Instant from, Instant to);

    /** Units produced by the batches completed in [from, to) (PRD-04); zero when there is none. */
    @Query("""
            select coalesce(sum(b.quantity), 0) from ProductionBatch b
            where b.completedAt >= :from and b.completedAt < :to
            """)
    BigDecimal sumCompletedQuantityBetween(Instant from, Instant to);

    /** Number of batches in the given status, whatever their date (PRD-04: batches in progress). */
    long countByStatus(ProductionBatchStatus status);

    /** Batches producing or consuming the article (ART-06), most recent first; the page gives how many. */
    @Query("""
            select b from ProductionBatch b
            where b.outputArticleId = :articleId
               or b.id in (select c.id from ProductionBatch c join c.inputs i where i.articleId = :articleId)
            order by b.createdAt desc, b.number desc
            """)
    List<ProductionBatch> findInvolving(UUID articleId, Pageable pageable);
}
