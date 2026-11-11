package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.dayclosing.domain.ClosingGapReason;

/**
 * Reconciliation of one article in one location for the day (JR-04). {@code opening} is the quantity held when the
 * day started; the six components are the day's movements, each as a positive quantity; {@code theoretical} is
 * {@code opening + transferred + returned - sold - delivered - salon - shooting}. {@code systemStock} is what the
 * stock ledger holds now (the starting point of the evening count) and {@code gap} is
 * {@code systemStock - theoretical}: non-zero when a movement outside the six components (inventory, production)
 * touched the location during the day. {@code counted} and {@code reason} are the evening count saved by the
 * administrator (JR-05), null until the line is counted; the reason is only present when the count differs from
 * {@code theoretical}.
 */
public record ClosingLineRDto(
        UUID articleId,
        String reference,
        String name,
        BigDecimal opening,
        BigDecimal transferred,
        BigDecimal returned,
        BigDecimal sold,
        BigDecimal delivered,
        BigDecimal salon,
        BigDecimal shooting,
        BigDecimal theoretical,
        BigDecimal systemStock,
        BigDecimal gap,
        @Nullable BigDecimal counted,
        @Nullable ClosingGapReason reason) {}
