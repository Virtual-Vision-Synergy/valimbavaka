package org.vvs.valimbavaka.feature.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLineTally;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** Lines are created with their session and never deleted. */
public interface InventoryLineRepository extends OperationRepository<InventoryLine, UUID> {

    <S extends InventoryLine> List<S> saveAll(Iterable<S> lines);

    Page<InventoryLine> findBySessionId(UUID sessionId, Pageable pageable);

    /**
     * Number of lines and of lines with a gap of each given session, in one query for a whole page (INV-05). A line
     * not counted has a null count: the comparison is unknown and the line is not a gap.
     */
    @Query("""
            select new org.vvs.valimbavaka.feature.inventory.domain.InventoryLineTally(
                l.sessionId, count(l), count(case when l.countedQuantity <> l.expectedQuantity then l.id end))
            from InventoryLine l
            where l.sessionId in :sessionIds
            group by l.sessionId
            """)
    List<InventoryLineTally> tallies(Collection<UUID> sessionIds);

    /** Every line of a session, by article code (INV-04: closing). */
    List<InventoryLine> findBySessionIdOrderByArticleCode(UUID sessionId);

    /** A line is only reachable through its own session (INV-02). */
    Optional<InventoryLine> findByIdAndSessionId(UUID id, UUID sessionId);
}
