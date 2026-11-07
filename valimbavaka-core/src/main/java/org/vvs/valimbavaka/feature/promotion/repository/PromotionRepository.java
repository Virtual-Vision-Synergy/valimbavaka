package org.vvs.valimbavaka.feature.promotion.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.promotion.domain.Promotion;

/** Extends {@link Repository}, not JpaRepository: no delete exposed. */
public interface PromotionRepository extends Repository<Promotion, UUID> {

    Promotion save(Promotion promotion);

    Optional<Promotion> findById(UUID id);

    /** Codes are stored in upper case: compare with an upper-case value. */
    boolean existsByCode(String code);
}
