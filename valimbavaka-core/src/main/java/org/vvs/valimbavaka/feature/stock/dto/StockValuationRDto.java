package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.List;

/** Valuation of the whole stock (STK-03): one entry per active location (zero included) and their totals. */
public record StockValuationRDto(
        BigDecimal quantity, BigDecimal value, BigDecimal unvaluedQuantity, List<StockLocationValueRDto> locations) {}
