package org.vvs.valimbavaka.feature.promotion.domain;

/**
 * Discount rule of a promotion. {@code PERCENT}: percentage off each eligible line (0 excluded, 100 max);
 * {@code AMOUNT}: amount in ariary off the total of the eligible lines; {@code BUY_X}: for N bought of the same
 * article, one is free (N whole, from 2 to 100).
 */
public enum PromotionKind {
    PERCENT,
    AMOUNT,
    BUY_X
}
