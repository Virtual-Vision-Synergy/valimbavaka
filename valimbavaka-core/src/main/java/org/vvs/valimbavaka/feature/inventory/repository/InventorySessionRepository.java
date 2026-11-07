package org.vvs.valimbavaka.feature.inventory.repository;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** A session is never deleted: it is closed (INV-04) or cancelled. */
public interface InventorySessionRepository extends OperationRepository<InventorySession, UUID> {

    boolean existsByLocationIdAndStatus(UUID locationId, InventoryStatus status);

    /** Next number of the session sequence (gaps are possible, a number is never reused). */
    @Query(value = "select nextval('inventory_session_number_seq')", nativeQuery = true)
    long nextNumber();

    /** Sessions filtered by status and location; a null filter means "all". */
    @Query("""
            select s from InventorySession s
            where (:status is null or s.status = :status)
              and (:locationId is null or s.locationId = :locationId)
            """)
    Page<InventorySession> search(@Nullable InventoryStatus status, @Nullable UUID locationId, Pageable pageable);
}
