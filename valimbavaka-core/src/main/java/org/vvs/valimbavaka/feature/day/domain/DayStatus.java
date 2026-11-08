package org.vvs.valimbavaka.feature.day.domain;

/** State of a business day: opened by the morning dispatch (JR-01), closed by the evening lock (JR-06). */
public enum DayStatus {
    /** Opened: sales, deliveries and salon services of the day can be recorded (VTE-01). */
    OPEN,
    /** Locked after the evening reconciliation (JR-04 to JR-06): nothing more is recorded for that date. */
    CLOSED
}
