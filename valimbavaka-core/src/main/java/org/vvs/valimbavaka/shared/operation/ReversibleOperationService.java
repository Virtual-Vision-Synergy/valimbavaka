package org.vvs.valimbavaka.shared.operation;

import java.util.UUID;

/**
 * Seules issues possibles pour une opération validée (JNL-02). Chaque implémentation trace l'action
 * ({@code CANCEL} ou {@code REVERSE}) dans le journal, dans la même transaction.
 *
 * @param <R> DTO de sortie de l'opération
 */
public interface ReversibleOperationService<R> {

    /** Annulation à chaud : l'opération passe à {@link OperationStatus#CANCELLED}. */
    R cancel(UUID operationId, String reason);

    /** Contre-passation : crée l'écriture inverse (avoir), l'originale passe à {@link OperationStatus#REVERSED}. */
    R reverse(UUID operationId, String reason);
}
