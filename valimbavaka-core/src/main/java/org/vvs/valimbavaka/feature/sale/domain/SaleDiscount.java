package org.vvs.valimbavaka.feature.sale.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Discount decided by the server for a sale (PRO-02).
 *
 * @param promotionId promotion that gives the discount, {@code null} when there is none
 * @param amount discount in ariary, 2 decimals, never above the subtotal (the sale caps it)
 */
public record SaleDiscount(@Nullable UUID promotionId, BigDecimal amount) {

    /** No discount: the total equals the subtotal. */
    public static final SaleDiscount NONE = new SaleDiscount(null, new BigDecimal("0.00"));
}
