package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Outcome of a recalculation (COM-05). {@code updatedCount} commissions changed, {@code unchangedCount} already matched
 * the rules in force, {@code validatedCount} were left alone because a validated commission is final. The totals cover
 * the commissions examined (updated or unchanged), before and after, in ariary.
 */
public record CommissionRecalculationRDto(
        LocalDate from,
        LocalDate to,
        @Nullable UUID sellerId,
        long updatedCount,
        long unchangedCount,
        long validatedCount,
        BigDecimal previousAmount,
        BigDecimal newAmount) {}
