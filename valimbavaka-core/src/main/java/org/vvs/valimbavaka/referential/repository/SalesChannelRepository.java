package org.vvs.valimbavaka.referential.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vvs.valimbavaka.referential.domain.SalesChannel;

import java.util.Optional;
import java.util.UUID;

public interface SalesChannelRepository extends JpaRepository<SalesChannel, UUID> {
    Optional<SalesChannel> findByCode(String code);
}
