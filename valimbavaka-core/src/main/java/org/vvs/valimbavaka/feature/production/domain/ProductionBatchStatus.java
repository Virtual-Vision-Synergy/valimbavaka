package org.vvs.valimbavaka.feature.production.domain;

import org.vvs.valimbavaka.core.exception.ConflictException;

/**
 * Life cycle of a production batch. PRD-01 only creates {@link #PLANNED} batches; the transitions belong to PRD-03
 * and the stock movements generated on completion to PRD-02.
 */
public enum ProductionBatchStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED;

    /** Completion (PRD-02): once only, the stock movements of a batch are never generated twice. */
    public void ensureCanComplete() {
        if (this == COMPLETED) {
            throw new ConflictException("Le lot est déjà validé : son stock a déjà été mouvementé.");
        }
    }
}
