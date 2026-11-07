package org.vvs.valimbavaka.feature.inventory.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryReasonWDto;

/** Qualification of the gap of the lines of an inventory session (INV-03). */
public interface InventoryReasonService {

    /**
     * Records the reason of the gap of a counted line and returns the line. 404 if the session or the line of this
     * session is unknown, 409 if the session is no longer in progress, 422 if the line is not counted or has no gap.
     * Traced in the journal when the reason changes.
     */
    InventoryLineRDto qualifyLine(UUID sessionId, UUID lineId, InventoryReasonWDto request);
}
