package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Production indicators of the current month of Madagascar (PRD-04). A batch counts when it is completed, dated by
 * {@code completedAt}; the material cost only covers the batches whose every input has a frozen cost (PRD-02), the
 * others are counted in {@code uncostedBatches}.
 *
 * @param month current month, {@code 2026-11}
 * @param completedBatches batches completed during the month
 * @param inProgressBatches batches currently in progress, whatever their month
 * @param unitsProduced sum of the output quantities of the batches completed during the month
 * @param previousMonthUnits same sum for the previous month
 * @param unitsTrendPercent rounded change of the units against the previous month, null when it produced nothing
 * @param materialCost sum of {@code quantity x unitCost} of the inputs of the fully costed batches of the month
 * @param materialCostPerUnit {@code materialCost} divided by the units of these batches, null when there is none
 * @param uncostedBatches batches of the month with at least one input without cost, left out of the material cost
 */
public record ProductionIndicatorsRDto(
        String month,
        long completedBatches,
        long inProgressBatches,
        BigDecimal unitsProduced,
        BigDecimal previousMonthUnits,
        @Nullable Integer unitsTrendPercent,
        BigDecimal materialCost,
        @Nullable BigDecimal materialCostPerUnit,
        long uncostedBatches) {}
