package org.vvs.valimbavaka.feature.article.domain;

/** What the bulk import does with a row whose code or barcode already belongs to an article (ART-07). */
public enum ImportConflictMode {
    /** The row is rejected and reported as a conflict; the existing article is left untouched. */
    SKIP,
    /** The existing article is replaced by the row (full replacement, like a PUT), traced in the journal. */
    OVERWRITE
}
