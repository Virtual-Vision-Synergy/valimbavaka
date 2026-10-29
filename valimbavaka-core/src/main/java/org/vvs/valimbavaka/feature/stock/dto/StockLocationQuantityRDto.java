package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Quantity of an article in one location; {@code code} is the stable referential code (e.g. WAREHOUSE). */
public record StockLocationQuantityRDto(UUID locationId, String code, String label, BigDecimal quantity) {}
