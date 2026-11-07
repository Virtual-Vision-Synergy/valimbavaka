package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;

/**
 * Detail and cost price of a batch (PRD-05). {@code estimated}: the batch is not completed yet, its costs are the
 * current purchase costs; once completed, they are those frozen at completion (PRD-02). {@code materialCost} = sum of
 * the line costs and {@code unitCost} = material cost per unit produced (2 decimals): both null as soon as one input
 * has no cost ({@code uncostedInputs} &gt; 0), never counted for 0. {@code marginPercent}: margin on the current sale
 * price of the output, rounded, null without sale price or without cost.
 */
public record ProductionBatchCostRDto(
        UUID id,
        String number,
        UUID outputArticleId,
        String outputArticleName,
        BigDecimal quantity,
        ProductionBatchStatus status,
        @Nullable Instant completedAt,
        boolean estimated,
        List<ProductionInputCostRDto> inputs,
        @Nullable BigDecimal materialCost,
        @Nullable BigDecimal unitCost,
        int uncostedInputs,
        @Nullable BigDecimal salePrice,
        @Nullable Integer marginPercent) {}
