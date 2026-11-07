package org.vvs.valimbavaka.feature.shooting.domain;

/**
 * Stored status of a shooting exit (SHO-01): out of the stock until its return (SHO-02). Being late is not stored:
 * it is computed from the expected return ({@link Shooting#isLateAt}).
 */
public enum ShootingStatus {
    OUT,
    RETURNED
}
