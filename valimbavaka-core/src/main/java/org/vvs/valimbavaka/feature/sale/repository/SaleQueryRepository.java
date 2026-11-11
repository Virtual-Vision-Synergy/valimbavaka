package org.vvs.valimbavaka.feature.sale.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.sale.domain.Sale;

/**
 * Read side of the sales (VTE-07): the list is paged on the ids, then the page is read with its lines in one query
 * (no N+1, no collection fetch paged in memory). Read only: no save, no delete (JNL-02).
 */
public interface SaleQueryRepository extends Repository<Sale, UUID> {

    /** Ids of the sales of one business day; a {@code null} channel or seller = all. */
    @Query(value = """
            select s.id from Sale s
            where s.businessDate = :businessDate
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
              and (:sellerId is null or s.sellerId = :sellerId)
            """, countQuery = """
            select count(s) from Sale s
            where s.businessDate = :businessDate
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
              and (:sellerId is null or s.sellerId = :sellerId)
            """)
    Page<UUID> searchIds(
            LocalDate businessDate, @Nullable UUID salesChannelId, @Nullable UUID sellerId, Pageable pageable);

    /** Sales with their lines, in no particular order. */
    @Query("select distinct s from Sale s left join fetch s.lines where s.id in :ids")
    List<Sale> findWithLinesByIdIn(Collection<UUID> ids);
}
