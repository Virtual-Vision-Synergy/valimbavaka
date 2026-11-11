package org.vvs.valimbavaka.feature.sale.domain;

/** How the customer pays a sale (VTE-01, VTE-05); {@code ON_DELIVERY} is the cash collected by the driver (LIV-03). */
public enum PaymentMethod {
    CASH("Espèces"),
    MVOLA("MVola"),
    ORANGE_MONEY("Orange Money"),
    CARD("Carte"),
    ON_DELIVERY("À la livraison");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    /** Label shown in the journal and on the invoice. */
    public String label() {
        return label;
    }
}
