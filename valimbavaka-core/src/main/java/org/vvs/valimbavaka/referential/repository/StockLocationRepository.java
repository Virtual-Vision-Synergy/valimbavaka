package org.vvs.valimbavaka.referential.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vvs.valimbavaka.referential.domain.StockLocation;

import java.util.Optional;
import java.util.UUID;

public interface StockLocationRepository extends JpaRepository<StockLocation, UUID> {
    Optional<StockLocation> findByCode(String code);
}
