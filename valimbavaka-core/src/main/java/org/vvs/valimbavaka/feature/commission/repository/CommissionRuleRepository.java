package org.vvs.valimbavaka.feature.commission.repository;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.commission.domain.CommissionRule;

/** Extends {@link Repository}, not JpaRepository: no delete exposed, a commission rule is deactivated. */
public interface CommissionRuleRepository extends Repository<CommissionRule, UUID> {

    CommissionRule save(CommissionRule rule);

    Optional<CommissionRule> findById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Active rule of a sales channel: at most one (partial unique index on the channel where active). */
    Optional<CommissionRule> findBySalesChannelIdAndActiveTrue(UUID salesChannelId);

    /** Case-insensitive search on the name; empty {@code search} = all, null filters = all. */
    @Query(value = """
            select r from CommissionRule r
            where lower(r.name) like lower(concat('%', :search, '%'))
              and (:salesChannelId is null or r.salesChannelId = :salesChannelId)
              and (:active is null or r.active = :active)
            """, countQuery = """
            select count(r) from CommissionRule r
            where lower(r.name) like lower(concat('%', :search, '%'))
              and (:salesChannelId is null or r.salesChannelId = :salesChannelId)
              and (:active is null or r.active = :active)
            """)
    Page<CommissionRule> search(
            String search, @Nullable UUID salesChannelId, @Nullable Boolean active, Pageable pageable);
}
