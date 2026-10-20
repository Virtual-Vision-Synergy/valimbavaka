package org.vvs.valimbavaka.shared.operation;

import org.vvs.valimbavaka.core.exception.ConflictException;

/**
 * Cycle de vie d'une opération métier validée (JNL-02) : une opération validée ne se supprime jamais, elle
 * s'annule à chaud ou se contre-passe (avoir).
 */
public enum OperationStatus {
    VALIDATED,
    CANCELLED,
    REVERSED;

    /** Annulation à chaud : réservée à une opération encore validée. */
    public void ensureCanCancel() {
        ensureValidated("annulée");
    }

    /** Contre-passation (avoir) : réservée à une opération encore validée. */
    public void ensureCanReverse() {
        ensureValidated("contre-passée");
    }

    private void ensureValidated(String verb) {
        if (this != VALIDATED) {
            throw new ConflictException("L'opération ne peut pas être " + verb + " : statut " + name());
        }
    }
}
