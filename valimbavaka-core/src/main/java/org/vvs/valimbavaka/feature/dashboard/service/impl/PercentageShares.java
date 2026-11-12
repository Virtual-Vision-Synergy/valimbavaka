package org.vvs.valimbavaka.feature.dashboard.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Shares in percent with one decimal that add up to exactly 100.0 (largest remainder method): rounding each share
 * alone could give 99.9 or 100.1. A zero (or negative) total gives only 0.0, never a division by zero.
 */
final class PercentageShares {

    /** Tenths of a percent in the whole. */
    private static final BigDecimal WHOLE = BigDecimal.valueOf(1000);

    private static final BigDecimal ZERO_SHARE = BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);

    private PercentageShares() {}

    /** One share per revenue, in the same order. */
    static List<BigDecimal> of(List<BigDecimal> revenues) {
        var total = revenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            return Collections.nCopies(revenues.size(), ZERO_SHARE);
        }
        var tenths = new ArrayList<Long>();
        var remainders = new ArrayList<BigDecimal>();
        for (var revenue : revenues) {
            var scaled = revenue.max(BigDecimal.ZERO).multiply(WHOLE);
            var floor = scaled.divideToIntegralValue(total);
            tenths.add(floor.longValue());
            remainders.add(scaled.subtract(floor.multiply(total)));
        }
        distributeLeftover(tenths, remainders);
        return tenths.stream().map(value -> BigDecimal.valueOf(value, 1)).toList();
    }

    /** Gives the tenths lost by rounding down, one each, to the largest remainders (the first one on a tie). */
    private static void distributeLeftover(List<Long> tenths, List<BigDecimal> remainders) {
        var leftover =
                WHOLE.longValue() - tenths.stream().mapToLong(Long::longValue).sum();
        var order = new ArrayList<>(IntStream.range(0, tenths.size()).boxed().toList());
        order.sort(Comparator.comparing(remainders::get, Comparator.reverseOrder()));
        for (var rank = 0; rank < leftover; rank++) {
            var index = order.get(rank);
            tenths.set(index, tenths.get(index) + 1);
        }
    }
}
