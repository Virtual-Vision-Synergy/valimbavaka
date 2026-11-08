package org.vvs.valimbavaka.feature.shooting.service;

import org.vvs.valimbavaka.feature.shooting.dto.ShootingSummaryRDto;

/** Follow-up of the shooting exits still out (SHO-03): how many, how many late, and the value they tie up. */
public interface ShootingSummaryService {

    /** Computed by aggregate queries over every exit still out, not over a page of the list. */
    ShootingSummaryRDto getSummary();
}
