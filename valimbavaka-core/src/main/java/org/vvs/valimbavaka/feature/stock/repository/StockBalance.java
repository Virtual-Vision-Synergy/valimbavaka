package org.vvs.valimbavaka.feature.stock.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Sum of the movements of an article in a location. */
public record StockBalance(UUID articleId, UUID locationId, BigDecimal quantity) {}
