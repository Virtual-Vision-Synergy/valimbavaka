package org.vvs.valimbavaka.feature.shooting.dto;

import java.math.BigDecimal;

/**
 * Indicators of the shooting screen (SHO-03), over the exits still out ({@code OUT}).
 *
 * @param outCount exits still out
 * @param lateCount exits still out after their expected return
 * @param immobilizedValue quantity times purchase cost of every article still out, in Ariary (2 decimals)
 * @param unvaluedQuantity quantity still out of articles without purchase cost, not included in the value
 */
public record ShootingSummaryRDto(
        long outCount, long lateCount, BigDecimal immobilizedValue, BigDecimal unvaluedQuantity) {}
