package org.vvs.valimbavaka.feature.promotion.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;

/** Extends {@link Repository}, not JpaRepository: no delete exposed. */
public interface PromotionRepository extends Repository<Promotion, UUID> {

    /** Value of {@code status} in {@link #search} meaning "every status". */
    String ANY_STATUS = "ALL";

    Promotion save(Promotion promotion);

    Optional<Promotion> findById(UUID id);

    /** Codes are stored in upper case: compare with an upper-case value. */
    boolean existsByCode(String code);

    /**
     * Case-insensitive search on name and code; empty {@code search} = all. {@code status} is {@link #ANY_STATUS} or
     * the name of a {@code PromotionStatus}, deduced from the period and {@code today} (same rule as
     * {@code PromotionPeriod.statusOn}).
     */
    @Query(value = """
            select p from Promotion p
            where (lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(p.code, '')) like lower(concat('%', :search, '%')))
              and (:status = 'ALL'
                   or (:status = 'SCHEDULED' and p.startsOn > :today)
                   or (:status = 'ACTIVE' and p.startsOn <= :today and p.endsOn >= :today)
                   or (:status = 'ENDED' and p.endsOn < :today))
            """, countQuery = """
            select count(p) from Promotion p
            where (lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(p.code, '')) like lower(concat('%', :search, '%')))
              and (:status = 'ALL'
                   or (:status = 'SCHEDULED' and p.startsOn > :today)
                   or (:status = 'ACTIVE' and p.startsOn <= :today and p.endsOn >= :today)
                   or (:status = 'ENDED' and p.endsOn < :today))
            """)
    Page<Promotion> search(String search, String status, LocalDate today, Pageable pageable);

    /**
     * Promotions active on {@code today} (same rule as {@code PromotionPeriod.statusOn}) that target one of
     * {@code targetIds} (an article and its range, ART-06) or have the {@code catalogue} scope; closest end first.
     */
    @Query("""
            select distinct p from Promotion p left join p.targetIds t
            where p.startsOn <= :today and p.endsOn >= :today
              and (p.scope = :catalogue or t in :targetIds)
            order by p.endsOn, p.name
            """)
    List<Promotion> findActiveTargeting(Collection<UUID> targetIds, PromotionScope catalogue, LocalDate today);

    /**
     * Automatic promotions (no code) active on {@code today} (same rule as {@code PromotionPeriod.statusOn}) whose
     * usage limit, if any, is not reached (PRO-02); closest end first, then by name.
     */
    @Query("""
            select p from Promotion p
            where p.code is null
              and p.startsOn <= :today and p.endsOn >= :today
              and (p.usageLimit is null or p.usageCount < p.usageLimit)
            order by p.endsOn, p.name
            """)
    List<Promotion> findAutomaticActive(LocalDate today);
}
