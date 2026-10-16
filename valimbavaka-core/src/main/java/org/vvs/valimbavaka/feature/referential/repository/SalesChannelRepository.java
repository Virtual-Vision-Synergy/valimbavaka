package org.vvs.valimbavaka.feature.referential.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.vvs.valimbavaka.feature.referential.domain.SalesChannel;

public interface SalesChannelRepository extends JpaRepository<SalesChannel, UUID> {

    Page<SalesChannel> findByActiveTrue(Pageable pageable);
}
