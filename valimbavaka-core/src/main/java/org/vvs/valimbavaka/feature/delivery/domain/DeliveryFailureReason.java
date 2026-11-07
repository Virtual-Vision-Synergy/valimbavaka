package org.vvs.valimbavaka.feature.delivery.domain;

/** Reason given by the driver when a delivery fails (LIV-04); always required, the comment stays optional. */
public enum DeliveryFailureReason {
    ABSENT("client absent"),
    ADDRESS_NOT_FOUND("adresse introuvable"),
    REFUSED("refusée par le client"),
    OTHER("autre motif");

    private final String label;

    DeliveryFailureReason(String label) {
        this.label = label;
    }

    /** French label used in the journal. */
    public String label() {
        return label;
    }
}
