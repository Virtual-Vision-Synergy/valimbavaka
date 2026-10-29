package org.vvs.valimbavaka.feature.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * One typed movement requested by a business feature (receipt, sale, transfer leg...).
 *
 * @param quantity signed, 3 decimals at most, never zero
 * @param referenceId business operation that caused the movement (sale, transfer...), if any
 */
public record StockMovementCommand(
        UUID articleId,
        UUID locationId,
        StockMovementType type,
        BigDecimal quantity,
        @Nullable UUID referenceId) {}
