package org.vvs.valimbavaka.feature.salonservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.salonservice.domain.SalonService;

/** Extends {@link Repository}, not JpaRepository: no delete exposed, a salon service is deactivated. */
public interface SalonServiceRepository extends Repository<SalonService, UUID> {

    SalonService save(SalonService service);

    Optional<SalonService> findById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Case-insensitive search on the name; empty {@code search} = all, null {@code active} = all. */
    @Query(value = """
            select s from SalonService s
            where lower(s.name) like lower(concat('%', :search, '%'))
              and (:active is null or s.active = :active)
            """, countQuery = """
            select count(s) from SalonService s
            where lower(s.name) like lower(concat('%', :search, '%'))
              and (:active is null or s.active = :active)
            """)
    Page<SalonService> search(String search, @Nullable Boolean active, Pageable pageable);
}
