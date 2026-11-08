package org.vvs.valimbavaka.feature.salonservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Read DTO of a salon service: price in ariary (two decimals), duration in minutes, commission rate in percent
 * ({@code null} = none of its own) and products usually consumed, sorted by article identifier.
 */
public record SalonServiceRDto(
        UUID id,
        String name,
        int durationMinutes,
        BigDecimal price,
        @Nullable BigDecimal commissionRate,
        List<ConsumedProductRDto> products,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
