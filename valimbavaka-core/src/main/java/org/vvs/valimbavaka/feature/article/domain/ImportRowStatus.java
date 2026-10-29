package org.vvs.valimbavaka.feature.article.domain;

/** Outcome of one row of the bulk import. */
public enum ImportRowStatus {
    CREATED,
    UPDATED,
    /** Overwrite requested but the row holds exactly what the article already has: nothing written, no journal. */
    UNCHANGED,
    /** Code or barcode already used (or repeated in the file): rejected, to be decided by the administrator. */
    CONFLICT,
    /** Malformed or invalid row: rejected. */
    INVALID
}
