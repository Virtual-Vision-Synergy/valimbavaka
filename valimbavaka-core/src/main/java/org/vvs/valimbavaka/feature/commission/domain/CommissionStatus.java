package org.vvs.valimbavaka.feature.commission.domain;

/** Life of a commission: computed with its operation (COM-02), cancelled with it (cancelled sale). */
public enum CommissionStatus {
    CALCULATED,
    CANCELLED
}
