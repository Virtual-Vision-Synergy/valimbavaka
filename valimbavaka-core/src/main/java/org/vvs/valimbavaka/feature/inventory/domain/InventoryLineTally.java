package org.vvs.valimbavaka.feature.inventory.domain;

import java.util.UUID;

/**
 * Lines of a session and, among them, the lines with a gap (counted, and count different from the theoretical stock
 * frozen at the count, as {@link InventoryLine#hasGap()}): read in one grouped query for a page of sessions (INV-05).
 */
public record InventoryLineTally(UUID sessionId, Long lineCount, Long gapCount) {

    /** Tally of a session with no line yet read (a session is never opened without lines). */
    public static InventoryLineTally none(UUID sessionId) {
        return new InventoryLineTally(sessionId, 0L, 0L);
    }
}
