package org.vvs.valimbavaka.feature.inventory.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;

/** Closing of an inventory session (INV-04): the stock of the location is adjusted to the counted quantities. */
public interface InventoryClosingService {

    /**
     * Adjusts the stock of every counted article whose current stock differs from its count (one {@code INVENTORY}
     * movement each, referencing the session), then closes the session; all in one transaction and traced in the
     * journal. 404 if the session is unknown, 409 if it is no longer in progress, 422 if a line is not counted or a
     * gap has no reason.
     */
    InventorySessionRDto closeSession(UUID sessionId);
}
