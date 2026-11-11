package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Quantity of an article held in a location at a given instant (JR-04). */
public record StockBalanceRDto(UUID articleId, UUID locationId, BigDecimal quantity) {}
