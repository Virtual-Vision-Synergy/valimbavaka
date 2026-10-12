package org.vvs.valimbavaka.referential.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vvs.valimbavaka.referential.domain.ProductRange;

import java.util.Optional;
import java.util.UUID;

public interface ProductRangeRepository extends JpaRepository<ProductRange, UUID> {
    Optional<ProductRange> findByCode(String code);
}
