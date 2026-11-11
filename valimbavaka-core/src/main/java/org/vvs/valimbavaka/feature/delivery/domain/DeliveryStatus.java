package org.vvs.valimbavaka.feature.delivery.domain;

import org.vvs.valimbavaka.core.exception.ConflictException;

/**
 * Life cycle of an order to deliver (LIV-01). LIV-01 only creates {@link #TO_PREPARE} orders; the transitions are
 * carried out by the following tickets (LIV-02 to LIV-05) through {@link DeliveryOrder#moveTo}:
 *
 * <ul>
 *   <li>{@code TO_PREPARE → ON_ROUND}: the driver leaves on his round;
 *   <li>{@code TO_PREPARE | ON_ROUND → DELIVERED}: delivered (LIV-03), the order becomes a sale with its invoice;
 *   <li>{@code TO_PREPARE | ON_ROUND → FAILED}: failure with a reason (LIV-04), the articles come back to stock;
 *   <li>{@code TO_PREPARE → CANCELLED}: cancelled before leaving.
 * </ul>
 *
 * {@link #DELIVERED}, {@link #FAILED} and {@link #CANCELLED} are final: only the time-limited undo of LIV-05 may
 * bring an order back to its previous status.
 */
public enum DeliveryStatus {
    TO_PREPARE("à préparer"),
    ON_ROUND("en tournée"),
    DELIVERED("livrée"),
    FAILED("en échec"),
    CANCELLED("annulée");

    private final String label;

    DeliveryStatus(String label) {
        this.label = label;
    }

    /** French label used in the error messages and the journal. */
    public String label() {
        return label;
    }

    /** True for a status that no ordinary transition can leave. */
    public boolean isFinal() {
        return this == DELIVERED || this == FAILED || this == CANCELLED;
    }

    /** True for the statuses that the time-limited undo of LIV-05 can leave: delivered or failed. */
    public boolean isUndoable() {
        return this == DELIVERED || this == FAILED;
    }

    /** Transition table of the order; a status never moves to itself. */
    public boolean canMoveTo(DeliveryStatus target) {
        return switch (this) {
            case TO_PREPARE -> target != TO_PREPARE;
            case ON_ROUND -> target == DELIVERED || target == FAILED;
            case DELIVERED, FAILED, CANCELLED -> false;
        };
    }

    /** 409 CONFLICT when the transition is not allowed, e.g. a delivered order cannot fail any more. */
    public void ensureCanMoveTo(DeliveryStatus target) {
        if (!canMoveTo(target)) {
            throw new ConflictException(
                    "La livraison ne peut pas passer de « %s » à « %s ».".formatted(label(), target.label()));
        }
    }
}
