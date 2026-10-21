package org.vvs.valimbavaka.feature.supplier.repository;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.vvs.valimbavaka.feature.supplier.domain.Supplier;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;

/** Extends {@link Repository}, not JpaRepository: no delete exposed, a supplier is deactivated. */
public interface SupplierRepository extends Repository<Supplier, UUID> {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Case-insensitive search on name and contact; empty {@code search} = all, null filters = all. */
    @Query(value = """
            select s from Supplier s
            where (lower(s.name) like lower(concat('%', :search, '%'))
                   or lower(s.contactName) like lower(concat('%', :search, '%')))
              and (:type is null or s.type = :type)
              and (:active is null or s.active = :active)
            """, countQuery = """
            select count(s) from Supplier s
            where (lower(s.name) like lower(concat('%', :search, '%'))
                   or lower(s.contactName) like lower(concat('%', :search, '%')))
              and (:type is null or s.type = :type)
              and (:active is null or s.active = :active)
            """)
    Page<Supplier> search(String search, @Nullable SupplierType type, @Nullable Boolean active, Pageable pageable);
}
