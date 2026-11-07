package org.vvs.valimbavaka.feature.shooting.repository;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.shooting.domain.Shooting;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A shooting exit is a validated operation (JNL-02): saved and read, never deleted. */
public interface ShootingRepository extends OperationRepository<Shooting, UUID> {

    /** Next number of the shooting sequence (gaps are possible, a number is never reused). */
    @Query(value = "select nextval('shooting_number_seq')", nativeQuery = true)
    long nextNumber();

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
}
