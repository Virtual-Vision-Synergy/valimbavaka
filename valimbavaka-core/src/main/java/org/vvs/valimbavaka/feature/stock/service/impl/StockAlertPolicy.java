package org.vvs.valimbavaka.feature.stock.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

import org.vvs.valimbavaka.feature.stock.dto.StockAlertRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;

/** Threshold alert rules (STK-04): which article is under its threshold, what is missing and what to order. */
final class StockAlertPolicy {

    /** Orders are suggested by tens: a shortage of 3 suggests 10, a shortage of 12.5 suggests 20. */
    static final BigDecimal ORDER_STEP = BigDecimal.TEN;

    private static final int RATIO_SCALE = 6;

    /** Most urgent first: lowest quantity over threshold ratio (out of stock = 0), then by code. */
    static final Comparator<StockAlertRDto> MOST_URGENT_FIRST =
            Comparator.comparing(StockAlertPolicy::coverage).thenComparing(StockAlertRDto::code);

    private StockAlertPolicy() {}

    /** A threshold of zero means "no alert"; a quantity equal to the threshold is not an alert. */
    static boolean isUnderThreshold(StockRDto stock) {
        return stock.threshold().signum() > 0 && stock.total().compareTo(stock.threshold()) < 0;
    }

    static StockAlertRDto toAlert(StockRDto stock) {
        var shortage = stock.threshold().subtract(stock.total());
        return new StockAlertRDto(
                stock.articleId(),
                stock.code(),
                stock.name(),
                stock.unit(),
                stock.total(),
                stock.threshold(),
                shortage,
                suggestedQuantity(shortage));
    }

    /** Shortage rounded up to the next {@link #ORDER_STEP}, never less than one step. */
    static BigDecimal suggestedQuantity(BigDecimal shortage) {
        var steps = shortage.divide(ORDER_STEP, 0, RoundingMode.CEILING).max(BigDecimal.ONE);
        return steps.multiply(ORDER_STEP);
    }

    private static BigDecimal coverage(StockAlertRDto alert) {
        return alert.quantity().divide(alert.threshold(), RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
