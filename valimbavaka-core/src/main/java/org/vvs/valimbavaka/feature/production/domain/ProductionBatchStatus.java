package org.vvs.valimbavaka.feature.production.domain;

import org.vvs.valimbavaka.core.exception.ConflictException;

/**
 * Life cycle of a production batch, forward only (PRD-03): {@link #PLANNED} (creation, PRD-01), {@link #IN_PROGRESS}
 * (start), {@link #COMPLETED} (completion with its stock movements, PRD-02, reachable from both previous statuses).
 */
public enum ProductionBatchStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED;

    /** Start (PRD-03): only a planned batch starts; the status never goes back. */
    public void ensureCanStart() {
        if (this == IN_PROGRESS) {
            throw new ConflictException("Le lot est déjà en cours.");
        }
        if (this == COMPLETED) {
            throw new ConflictException("Le lot est déjà terminé : son statut ne change plus.");
        }
    }

    /** Completion (PRD-02): once only, the stock movements of a batch are never generated twice. */
    public void ensureCanComplete() {
        if (this == COMPLETED) {
            throw new ConflictException("Le lot est déjà validé : son stock a déjà été mouvementé.");
        }
    }
}
