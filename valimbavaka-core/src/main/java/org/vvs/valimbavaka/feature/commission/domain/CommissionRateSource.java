package org.vvs.valimbavaka.feature.commission.domain;

/**
 * Level the applicable rate comes from, from the most precise to the least precise (PO decision of 07/11/2026: the
 * most precise wins, rates never add up). {@code NONE}: no level gives a rate, the commission is zero.
 */
public enum CommissionRateSource {
    SALON_SERVICE,
    SALES_CHANNEL,
    SELLER,
    NONE
}
