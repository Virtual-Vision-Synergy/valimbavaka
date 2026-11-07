package org.vvs.valimbavaka.feature.inventory.domain;

/** Life cycle of an inventory session: opened (INV-01), then closed (INV-04) or cancelled. */
public enum InventoryStatus {
    IN_PROGRESS,
    CLOSED,
    CANCELLED
}
