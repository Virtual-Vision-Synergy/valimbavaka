package org.vvs.valimbavaka.feature.journal.domain;

/** Nature d'une opération métier tracée. */
public enum JournalAction {
    CREATE,
    UPDATE,
    DELETE,
    CANCEL,
    REVERSE
}
