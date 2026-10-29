package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

public record StockMovementRDto(
        UUID id,
        UUID articleId,
        UUID locationId,
        StockMovementType type,
        BigDecimal quantity,
        @Nullable UUID referenceId,
        Instant occurredAt) {}
