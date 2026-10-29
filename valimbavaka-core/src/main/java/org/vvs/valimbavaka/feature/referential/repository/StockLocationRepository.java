package org.vvs.valimbavaka.feature.referential.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.vvs.valimbavaka.feature.referential.domain.StockLocation;

public interface StockLocationRepository extends JpaRepository<StockLocation, UUID> {

    Page<StockLocation> findByActiveTrue(Pageable pageable);

    boolean existsByIdAndActiveTrue(UUID id);
}
