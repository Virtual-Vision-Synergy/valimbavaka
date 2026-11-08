package org.vvs.valimbavaka.feature.promotion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionStatus;

/**
 * Promotion sheet. {@code status} is deduced from the period on the current business day; {@code usageCount} is
 * incremented by the till (PRO-02, VTE-04, followed by PRO-03).
 */
public record PromotionRDto(
        UUID id,
        String name,
        @Nullable String description,
        PromotionKind kind,
        BigDecimal value,
        @Nullable String code,
        PromotionScope scope,
        Set<UUID> targetIds,
        LocalDate startsOn,
        LocalDate endsOn,
        Set<UUID> channelIds,
        @Nullable Integer usageLimit,
        int usageCount,
        PromotionStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
