package org.vvs.valimbavaka.feature.inventory.domain;

/** Origin of a gap between the counted and the expected quantity (qualified by INV-03). */
public enum InventoryReason {
    BREAKAGE,
    LOSS,
    ENTRY_ERROR,
    UNDECLARED_SHOOTING,
    OTHER
}
