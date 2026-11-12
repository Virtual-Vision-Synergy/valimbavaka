package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Turnover split by product range (RPT-04): the ranges that sold something, biggest first, and the sum of their
 * turnovers. The shares add up to exactly 100.0; the list is empty (and the revenue 0) when nothing was sold.
 */
public record ReportRangesRDto(BigDecimal revenue, List<ReportRangeRDto> items) {}
