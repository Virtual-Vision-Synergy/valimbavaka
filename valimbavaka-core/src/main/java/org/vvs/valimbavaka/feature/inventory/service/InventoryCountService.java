package org.vvs.valimbavaka.feature.inventory.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.inventory.dto.InventoryCountWDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;

/** Physical count of the lines of an inventory session (INV-02). */
public interface InventoryCountService {

    /**
     * Records the counted quantity of a line and freezes the theoretical stock of the location at this instant;
     * returns the line with its gap (counted − theoretical). 404 if the session or the line of this session is
     * unknown, 409 if the session is no longer in progress. Traced in the journal when the count changes.
     */
    InventoryLineRDto countLine(UUID sessionId, UUID lineId, InventoryCountWDto request);
}
