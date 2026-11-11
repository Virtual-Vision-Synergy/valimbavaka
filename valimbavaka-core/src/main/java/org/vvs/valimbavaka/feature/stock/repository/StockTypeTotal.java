package org.vvs.valimbavaka.feature.stock.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;

/** Sum of the movements of one type for an article in a location, over a period. */
public record StockTypeTotal(UUID articleId, UUID locationId, StockMovementType type, BigDecimal quantity) {}
