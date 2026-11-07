package org.vvs.valimbavaka.feature.sale.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A line whose prices are fixed at the time of the sale.
 *
 * @param unitCost purchase cost of the article at the time of the sale (margin, COM-01 base {@code MARGIN}), if known
 */
public record PricedSaleLine(
        UUID articleId, BigDecimal quantity, BigDecimal unitPrice, @Nullable BigDecimal unitCost) {}
