package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Salon consumption of today set against the closing (SRV-04). {@code totalConsumed} adds the products of the
 * {@code servicesCount} performed services; {@code totalInClosing} adds the « Salon » column of the closing
 * (JR-04). {@code matches} is true when every article has the same quantity on both sides, so that the two totals
 * are equal; otherwise the lines with a non-zero {@code gap} show where they differ.
 */
public record ClosingSalonRDto(
        LocalDate businessDate,
        int servicesCount,
        BigDecimal totalConsumed,
        BigDecimal totalInClosing,
        boolean matches,
        List<ClosingSalonLineRDto> lines) {}
