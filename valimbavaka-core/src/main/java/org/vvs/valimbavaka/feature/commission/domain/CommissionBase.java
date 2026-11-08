package org.vvs.valimbavaka.feature.commission.domain;

/**
 * Amount the rate applies to (COM-01): the revenue of the sale (after discounts) or its gross margin (revenue minus
 * purchase cost). The calculation itself belongs to COM-02.
 */
public enum CommissionBase {
    REVENUE,
    MARGIN
}
