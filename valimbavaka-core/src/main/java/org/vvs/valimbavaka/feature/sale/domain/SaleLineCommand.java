package org.vvs.valimbavaka.feature.sale.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * One article sold.
 *
 * @param quantity strictly positive, 3 decimals at most
 * @param unitPrice price agreed beforehand (an order delivered by LIV-03), 2 decimals at most; {@code null} = the
 *     current sale price of the article, which is always the case for a sale entered at the counter
 */
public record SaleLineCommand(
        UUID articleId,
        BigDecimal quantity,
        @Nullable BigDecimal unitPrice) {}
