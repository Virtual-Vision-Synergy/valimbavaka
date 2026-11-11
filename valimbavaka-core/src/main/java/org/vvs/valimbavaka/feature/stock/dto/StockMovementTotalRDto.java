package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

/** Signed sum of the movements of one type for an article in a location over a period (JR-04). */
public record StockMovementTotalRDto(UUID articleId, UUID locationId, StockMovementType type, BigDecimal quantity) {}
