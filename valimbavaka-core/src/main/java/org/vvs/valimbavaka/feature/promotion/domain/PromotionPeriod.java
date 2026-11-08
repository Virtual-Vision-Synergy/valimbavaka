package org.vvs.valimbavaka.feature.promotion.domain;

import java.time.LocalDate;

/** Validity period, both days included, in the business time zone (Indian/Antananarivo). */
public record PromotionPeriod(LocalDate startsOn, LocalDate endsOn) {

    /** True if the end precedes the start. */
    public boolean isReversed() {
        return endsOn.isBefore(startsOn);
    }

    /** Status of a promotion with this period on the given business day. */
    public PromotionStatus statusOn(LocalDate day) {
        if (day.isBefore(startsOn)) {
            return PromotionStatus.SCHEDULED;
        }
        return day.isAfter(endsOn) ? PromotionStatus.ENDED : PromotionStatus.ACTIVE;
    }
}
