package org.vvs.valimbavaka.feature.promotion.domain;

/** Status deduced from the period and the business day (never stored): before, during or after the period. */
public enum PromotionStatus {
    SCHEDULED,
    ACTIVE,
    ENDED
}
