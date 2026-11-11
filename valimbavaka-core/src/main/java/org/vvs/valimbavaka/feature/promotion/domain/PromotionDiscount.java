package org.vvs.valimbavaka.feature.promotion.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Discount retained for a basket (PRO-02).
 *
 * @param promotionId the promotion that gives it; {@code null} when no promotion applies
 * @param promotionName name of that promotion, {@code null} when none applies
 * @param amount discount in ariary, 2 decimals, zero when no promotion applies
 */
public record PromotionDiscount(
        @Nullable UUID promotionId, @Nullable String promotionName, BigDecimal amount) {

    /** No promotion applies. */
    public static final PromotionDiscount NONE = new PromotionDiscount(null, null, new BigDecimal("0.00"));
}
