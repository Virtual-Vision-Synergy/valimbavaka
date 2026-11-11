package org.vvs.valimbavaka.feature.stock.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
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

    /** Non-zero balances of every article (inactive ones included) in every location (STK-03: valuation). */
    @Query("""
            select new org.vvs.valimbavaka.feature.stock.repository.StockBalance(
                m.articleId, m.locationId, sum(m.quantity))
            from StockMovement m
            group by m.articleId, m.locationId
            having sum(m.quantity) <> 0
            """)
    List<StockBalance> nonZeroBalances();

    /** Movements of {@code [from, to[} in the given locations, summed by article, location and type (JR-04). */
    @Query("""
            select new org.vvs.valimbavaka.feature.stock.repository.StockTypeTotal(
                m.articleId, m.locationId, m.type, sum(m.quantity))
            from StockMovement m
            where m.occurredAt >= :from and m.occurredAt < :to and m.locationId in :locationIds
            group by m.articleId, m.locationId, m.type
            """)
    List<StockTypeTotal> totalsByType(Instant from, Instant to, Collection<UUID> locationIds);

    /** Non-zero balances in the given locations of the movements recorded strictly before {@code before} (JR-04). */
    @Query("""
            select new org.vvs.valimbavaka.feature.stock.repository.StockBalance(
                m.articleId, m.locationId, sum(m.quantity))
            from StockMovement m
            where m.occurredAt < :before and m.locationId in :locationIds
            group by m.articleId, m.locationId
            having sum(m.quantity) <> 0
            """)
    List<StockBalance> balancesBefore(Instant before, Collection<UUID> locationIds);

    /** Movements of one article, most recent first; the page gives how many (ART-06: journal of the article). */
    List<StockMovement> findByArticleIdOrderByOccurredAtDescIdDesc(UUID articleId, Pageable pageable);

    /** Serialises concurrent movements on the same article and location until the end of the transaction. */
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(:key, 0)) is not null", nativeQuery = true)
    boolean lock(String key);
}
