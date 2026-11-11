package org.vvs.valimbavaka.feature.promotion.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Discount rule of a promotion on a basket (PRO-02, rules fixed by the PRO-01 guide). Each amount is rounded to the
 * ariary, half up (convention §6.4), and never exceeds what it is taken from, so the net is never negative:
 * <ul>
 *   <li>{@code PERCENT}: value % of each eligible line;</li>
 *   <li>{@code AMOUNT}: value ariary taken once off the total of the eligible lines;</li>
 *   <li>{@code BUY_X}: for each eligible line, {@code floor(quantity / value)} units are free.</li>
 * </ul>
 * The till (front) applies the same rule on the same examples; the server decides.
 */
public final class PromotionPricing {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PromotionPricing() {}

    /** Discount of {@code promotion} on the lines of {@code basket} it covers, 2 decimals; zero if none is covered. */
    public static BigDecimal discountOf(Promotion promotion, List<PromotionBasketLine> basket) {
        var eligible = basket.stream()
                .filter(line -> promotion.covers(line.articleId(), line.rangeId()))
                .toList();
        return amountOf(promotion, eligible).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal amountOf(Promotion promotion, List<PromotionBasketLine> eligible) {
        var value = promotion.getValue();
        return switch (promotion.getKind()) {
            case PERCENT -> sum(eligible, line -> percentOff(line, value));
            case AMOUNT -> amountOff(eligible, value);
            case BUY_X -> sum(eligible, line -> freeUnits(line, value));
        };
    }

    private static BigDecimal amountOff(List<PromotionBasketLine> eligible, BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).min(sum(eligible, PromotionBasketLine::total));
    }

    private static BigDecimal sum(List<PromotionBasketLine> lines, Function<PromotionBasketLine, BigDecimal> amount) {
        return lines.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal percentOff(PromotionBasketLine line, BigDecimal rate) {
        var total = line.total();
        return total.multiply(rate).divide(HUNDRED, 0, RoundingMode.HALF_UP).min(total);
    }

    private static BigDecimal freeUnits(PromotionBasketLine line, BigDecimal bought) {
        var free = line.quantity().divideToIntegralValue(bought);
        var unitPrice = line.unitPrice().setScale(2, RoundingMode.HALF_UP);
        return free.multiply(unitPrice).setScale(0, RoundingMode.HALF_UP).min(line.total());
    }
}
