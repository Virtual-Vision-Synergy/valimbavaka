package org.vvs.valimbavaka.feature.shooting.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;

/**
 * Shooting exit. {@code status} is the stored status ({@code OUT} until the return of SHO-02); {@code late} is true
 * while the exit is still out after {@code expectedReturnAt}.
 *
 * @param takenById account that recorded the exit
 * @param takenBy full name of that account when the exit was recorded
 */
public record ShootingRDto(
        UUID id,
        String number,
        UUID locationId,
        List<ShootingItemRDto> items,
        @Nullable UUID takenById,
        String takenBy,
        Instant expectedReturnAt,
        @Nullable String note,
        ShootingStatus status,
        boolean late,
        Instant createdAt) {}
