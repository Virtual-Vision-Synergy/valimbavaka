package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One article of the salon consumption of the day (SRV-04): {@code consumed} is the total of the products of the
 * performed services, {@code inClosing} the « Salon » column of the closing for the same article, {@code gap} is
 * {@code inClosing - consumed}. Quantities are positive.
 */
public record ClosingSalonLineRDto(
        UUID articleId, String reference, String name, BigDecimal consumed, BigDecimal inClosing, BigDecimal gap) {}
