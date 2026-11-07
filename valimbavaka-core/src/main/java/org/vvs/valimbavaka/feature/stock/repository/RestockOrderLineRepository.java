package org.vvs.valimbavaka.feature.stock.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

import org.vvs.valimbavaka.feature.stock.domain.RestockOrderLine;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** Lines of the restock orders (STK-05): written once with their order, never modified nor deleted. */
public interface RestockOrderLineRepository extends OperationRepository<RestockOrderLine, UUID> {

    <S extends RestockOrderLine> List<S> saveAll(Iterable<S> lines);

    /** Lines of several orders in one query, by order then by line number. */
    @Query("""
            select l from RestockOrderLine l
            where l.restockOrderId in :orderIds
            order by l.restockOrderId, l.lineNumber
            """)
    List<RestockOrderLine> findLinesOf(Collection<UUID> orderIds);
}
