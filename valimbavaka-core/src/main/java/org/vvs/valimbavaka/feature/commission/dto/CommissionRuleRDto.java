package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;

/** Read DTO of a commission rule; {@code rate} is a percentage with two decimals. */
public record CommissionRuleRDto(
        UUID id,
        String name,
        UUID salesChannelId,
        BigDecimal rate,
        CommissionBase base,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
