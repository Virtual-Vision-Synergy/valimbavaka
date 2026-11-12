package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Turnover of one product range over a period (RPT-04): the sum of the line totals of its articles (before the
 * discount of the whole sale) and its share of the turnover of all the ranges, in percent with one decimal.
 */
public record ReportRangeRDto(UUID rangeId, String code, String label, BigDecimal revenue, BigDecimal share) {}
