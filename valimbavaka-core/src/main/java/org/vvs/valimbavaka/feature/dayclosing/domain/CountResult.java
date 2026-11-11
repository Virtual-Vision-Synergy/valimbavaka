package org.vvs.valimbavaka.feature.dayclosing.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

/** What one count says: the counted and the theoretical quantity, the reason given for a gap, and when (JR-05). */
public record CountResult(
        BigDecimal counted, BigDecimal theoretical, @Nullable ClosingGapReason reason, Instant countedAt) {

    /** True when the counted quantity differs from the theoretical one. */
    public boolean hasGap() {
        return counted.compareTo(theoretical) != 0;
    }

    /** True when a line already holding these values would not change (the reason only counts with a gap). */
    boolean sameAs(BigDecimal otherCounted, BigDecimal otherTheoretical, @Nullable ClosingGapReason otherReason) {
        var expectedReason = hasGap() ? reason : null;
        return counted.compareTo(otherCounted) == 0
                && theoretical.compareTo(otherTheoretical) == 0
                && expectedReason == otherReason;
    }
}
