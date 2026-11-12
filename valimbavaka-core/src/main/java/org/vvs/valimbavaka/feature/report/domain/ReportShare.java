package org.vvs.valimbavaka.feature.report.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/** Share of each amount in their total, in percent with one decimal, adding up to exactly 100.0 (RPT-04). */
public final class ReportShare {

    private static final long TOTAL_TENTHS = 1000;
    private static final BigDecimal TENTHS_OF_PERCENT = BigDecimal.valueOf(TOTAL_TENTHS);
    private static final int PERCENT_SCALE = 1;

    private ReportShare() {}

    /**
     * Largest remainder method: every share is first rounded down to a tenth of a percent, then the tenths that are
     * missing to reach 100.0 go one by one to the shares that lost the most (the earlier amount wins a tie). The
     * shares of a list whose total is 0 are all 0.0 (no division by zero); a negative amount counts as 0.
     */
    public static List<BigDecimal> distribute(List<BigDecimal> amounts) {
        var values = amounts.stream().map(amount -> amount.max(BigDecimal.ZERO)).toList();
        var total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            return Collections.nCopies(amounts.size(), BigDecimal.valueOf(0, PERCENT_SCALE));
        }
        var tenths = new ArrayList<Long>();
        var remainders = new ArrayList<BigDecimal>();
        for (var value : values) {
            var division = value.multiply(TENTHS_OF_PERCENT).divideAndRemainder(total);
            tenths.add(division[0].longValueExact());
            remainders.add(division[1]);
        }
        giveMissingTenths(tenths, remainders);
        return tenths.stream().map(count -> BigDecimal.valueOf(count, PERCENT_SCALE)).toList();
    }

    private static void giveMissingTenths(List<Long> tenths, List<BigDecimal> remainders) {
        var missing = TOTAL_TENTHS - tenths.stream().mapToLong(Long::longValue).sum();
        var byLargestRemainder = IntStream.range(0, tenths.size())
                .boxed()
                .sorted(Comparator.comparing(remainders::get, Comparator.reverseOrder()))
                .limit(missing)
                .toList();
        for (var index : byLargestRemainder) {
            tenths.set(index, tenths.get(index) + 1);
        }
    }
}
