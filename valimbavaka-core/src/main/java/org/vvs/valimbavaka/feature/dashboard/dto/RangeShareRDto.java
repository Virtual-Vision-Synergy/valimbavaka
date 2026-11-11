package org.vvs.valimbavaka.feature.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One product range of the revenue split (DSH-04): turnover of the lines of its articles (validated sales of the day,
 * before the discount of the whole sale) and share of the turnover of all ranges in percent, one decimal.
 */
public record RangeShareRDto(UUID rangeId, String code, String label, BigDecimal revenue, BigDecimal sharePercent) {}
