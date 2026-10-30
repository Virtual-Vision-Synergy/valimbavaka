package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stock of one location, all articles together (STK-03): quantity, value at purchase cost in Ariary (2 decimals) and
 * the part of the quantity left out of the value because its article has no cost.
 */
public record StockLocationValueRDto(
        UUID locationId,
        String code,
        String label,
        BigDecimal quantity,
        BigDecimal value,
        BigDecimal unvaluedQuantity) {}
