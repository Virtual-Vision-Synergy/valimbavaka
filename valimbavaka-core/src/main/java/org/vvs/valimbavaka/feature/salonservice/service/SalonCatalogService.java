package org.vvs.valimbavaka.feature.salonservice.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Catalogue of salon services (SRV-01): read by the administrator and the seller, written by the administrator. */
public interface SalonCatalogService {

    PageResponse<SalonServiceRDto> getServices(PageQuery query, @Nullable Boolean active);

    SalonServiceRDto getServiceById(UUID serviceId);

    /**
     * Creates an active salon service.
     *
     * @throws org.vvs.valimbavaka.core.exception.ConflictException if the name is already used (case ignored)
     * @throws org.vvs.valimbavaka.core.exception.BusinessException if a consumed article is unknown, inactive or
     *         listed twice
     */
    SalonServiceRDto createService(SalonServiceWDto request);

    /** Updates a salon service; same checks as {@link #createService}, plus 409 on a stale version. */
    SalonServiceRDto updateService(UUID serviceId, SalonServiceWDto request);

    /** Activates or deactivates a salon service (idempotent: no trace if the state is already the requested one). */
    SalonServiceRDto setActive(UUID serviceId, boolean active);
}
