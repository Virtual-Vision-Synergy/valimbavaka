package org.vvs.valimbavaka.feature.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jspecify.annotations.Nullable;

/** Variation of an indicator against the previous period, in percent (RPT-02). */
public final class ReportVariation {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENT_SCALE = 1;

    private ReportVariation() {}

    /**
     * {@code (current - previous) / previous * 100}, rounded half up to one decimal; {@code null} when the previous
     * value is 0 (no percentage can be given: a rise from nothing is not a percentage).
     */
    public static @Nullable BigDecimal percent(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(HUNDRED).divide(previous, PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    /** Same as {@link #percent(BigDecimal, BigDecimal)} for two counts. */
    public static @Nullable BigDecimal percent(long current, long previous) {
        return percent(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
}
