package org.vvs.valimbavaka.feature.stock.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.stock.domain.StockMovement;

/** Extends {@link Repository}, not JpaRepository: append-only, no update or delete is exposed. */
public interface StockMovementRepository extends Repository<StockMovement, UUID> {

    StockMovement save(StockMovement movement);

    /** Balance of one article in one location (zero when it never moved). */
    @Query("""
            select coalesce(sum(m.quantity), 0) from StockMovement m
            where m.articleId = :articleId and m.locationId = :locationId
            """)
    BigDecimal balance(UUID articleId, UUID locationId);

    /** Balances of the given articles, one row per article and location that ever moved. */
    @Query("""
            select new org.vvs.valimbavaka.feature.stock.repository.StockBalance(
                m.articleId, m.locationId, sum(m.quantity))
            from StockMovement m
            where m.articleId in :articleIds
            group by m.articleId, m.locationId
            """)
    List<StockBalance> balances(Collection<UUID> articleIds);

    /** Serialises concurrent movements on the same article and location until the end of the transaction. */
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(:key, 0)) is not null", nativeQuery = true)
    boolean lock(String key);
}
