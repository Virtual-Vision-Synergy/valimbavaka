package org.vvs.valimbavaka.feature.commission.domain;

/**
 * Life of a commission: computed with its operation (COM-02), validated by the administrator before payout (COM-03),
 * or cancelled with its operation (cancelled sale). A validated commission whose sale is cancelled afterwards becomes
 * {@code CANCELLED} and keeps its validation date, so that the payout can be corrected.
 */
public enum CommissionStatus {
    CALCULATED,
    VALIDATED,
    CANCELLED
}
