package org.vvs.valimbavaka.feature.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** Lines are created with their session and never deleted. */
public interface InventoryLineRepository extends OperationRepository<InventoryLine, UUID> {

    <S extends InventoryLine> List<S> saveAll(Iterable<S> lines);

    Page<InventoryLine> findBySessionId(UUID sessionId, Pageable pageable);

    long countBySessionId(UUID sessionId);
}
