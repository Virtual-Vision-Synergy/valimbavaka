package org.vvs.valimbavaka.feature.shooting.repository;

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

import org.vvs.valimbavaka.feature.shooting.domain.Shooting;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A shooting exit is a validated operation (JNL-02): saved and read, never deleted. */
public interface ShootingRepository extends OperationRepository<Shooting, UUID> {

    /** Next number of the shooting sequence (gaps are possible, a number is never reused). */
    @Query(value = "select nextval('shooting_number_seq')", nativeQuery = true)
    long nextNumber();

    /** Exit locked in writing until the end of the transaction: two returns of one exit run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shooting s where s.id = :id")
    Optional<Shooting> findByIdForUpdate(UUID id);

    /** Search on number and author; empty {@code search} = all, null {@code status} = every status. */
    @Query(value = """
            select s from Shooting s
            where (lower(s.number) like lower(concat('%', :search, '%'))
                   or lower(s.takenByName) like lower(concat('%', :search, '%')))
              and (:status is null or s.status = :status)
            """, countQuery = """
            select count(s) from Shooting s
            where (lower(s.number) like lower(concat('%', :search, '%'))
                   or lower(s.takenByName) like lower(concat('%', :search, '%')))
              and (:status is null or s.status = :status)
            """)
    Page<Shooting> search(String search, @Nullable ShootingStatus status, Pageable pageable);

    /** Number of exits with this status (SHO-03: exits still out). */
    long countByStatus(ShootingStatus status);

    /** Number of exits with this status whose expected return is before {@code instant} (SHO-03: late exits). */
    long countByStatusAndExpectedReturnAtBefore(ShootingStatus status, Instant instant);

    /** Quantity of each article over every exit with this status (SHO-03: value tied up by the exits still out). */
    @Query("""
            select new org.vvs.valimbavaka.feature.shooting.repository.ShootingArticleQuantity(
                i.articleId, sum(i.quantity))
            from Shooting s join s.items i
            where s.status = :status
            group by i.articleId
            """)
    List<ShootingArticleQuantity> quantitiesByArticle(ShootingStatus status);
}
