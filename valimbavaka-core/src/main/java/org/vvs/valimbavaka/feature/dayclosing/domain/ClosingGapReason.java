package org.vvs.valimbavaka.feature.dayclosing.domain;

/** Origin of a gap between the counted and the theoretical quantity at closing (JR-05). */
public enum ClosingGapReason {
    BREAKAGE,
    LOSS,
    ENTRY_ERROR,
    SAMPLE,
    OTHER
}
