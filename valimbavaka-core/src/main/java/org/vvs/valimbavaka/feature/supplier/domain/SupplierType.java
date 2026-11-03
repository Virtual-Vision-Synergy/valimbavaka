package org.vvs.valimbavaka.feature.supplier.domain;

/** Supplier origin: local (Madagascar) or foreign. The origin gives the default restocking lead time (FRS-04). */
public enum SupplierType {
    LOCAL(7),
    FOREIGN(30);

    private final int defaultLeadTimeDays;

    SupplierType(int defaultLeadTimeDays) {
        this.defaultLeadTimeDays = defaultLeadTimeDays;
    }

    /** Lead time used when the sheet does not give one: one week for a local supplier, a month from abroad. */
    public int defaultLeadTimeDays() {
        return defaultLeadTimeDays;
    }
}
