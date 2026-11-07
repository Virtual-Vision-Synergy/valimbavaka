package org.vvs.valimbavaka.feature.stock.domain;

/** Lifecycle of a restock order: generated {@code PENDING} (STK-05), {@code RECEIVED} once delivered (STK-06). */
public enum RestockOrderStatus {
    PENDING,
    RECEIVED
}
