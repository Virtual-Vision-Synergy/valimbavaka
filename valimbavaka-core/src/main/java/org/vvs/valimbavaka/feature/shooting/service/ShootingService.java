package org.vvs.valimbavaka.feature.shooting.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingRDto;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Shooting exits (SHO-01): products taken out of the stock for a photo shooting, not counted as losses. */
public interface ShootingService {

    /** Most recent first; {@code status} null = every status. */
    PageResponse<ShootingRDto> getShootings(PageQuery query, @Nullable ShootingStatus status);

    ShootingRDto getShootingById(UUID shootingId);

    /**
     * Records the exit with status {@code OUT} and one {@code SHOOTING} stock movement per article, in one
     * transaction: 404 if an article is unknown, 422 if the location is unknown or inactive, the expected return is
     * not in the future or an article appears twice, 409 if a location would end up with a negative stock.
     */
    ShootingRDto createShooting(ShootingWDto request);
}
