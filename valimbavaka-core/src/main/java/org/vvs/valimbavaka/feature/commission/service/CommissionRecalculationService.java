package org.vvs.valimbavaka.feature.commission.service;

import org.vvs.valimbavaka.feature.commission.dto.CommissionRecalculationRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRecalculationWDto;

/**
 * Recalculation of the commissions of a period by the administrator (COM-05), after a rate or a rule changed. It uses
 * the rate of the calculation (COM-02): the most precise level wins, rates never add up (PO decision of 07/11/2026).
 */
public interface CommissionRecalculationService {

    /**
     * Recalculates every {@code CALCULATED} commission over the period, in one transaction, and traces each change in
     * the journal. A {@code VALIDATED} commission is final (COM-03) and a {@code CANCELLED} one no longer counts: both
     * are left untouched. Idempotent: a second run changes nothing and writes nothing to the journal. 422 if
     * {@code from} is after {@code to} or if the period is longer than a year.
     */
    CommissionRecalculationRDto recalculate(CommissionRecalculationWDto request);
}
