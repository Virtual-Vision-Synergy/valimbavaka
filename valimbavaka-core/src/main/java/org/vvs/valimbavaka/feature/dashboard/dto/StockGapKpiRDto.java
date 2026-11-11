package org.vvs.valimbavaka.feature.dashboard.dto;

import java.math.BigDecimal;

/**
 * Stock gap of today's closing reconciliation (JR-04, JR-05): {@code units} is the signed sum of the gaps of every
 * article line (negative when stock is missing), {@code articles} the number of distinct articles with a gap.
 */
public record StockGapKpiRDto(BigDecimal units, long articles) {}
