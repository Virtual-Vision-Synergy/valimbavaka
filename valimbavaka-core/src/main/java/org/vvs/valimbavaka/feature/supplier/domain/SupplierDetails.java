package org.vvs.valimbavaka.feature.supplier.domain;

import org.jspecify.annotations.Nullable;

/** Editable supplier sheet data, grouped to respect the parameter limit. */
public record SupplierDetails(
        String name,
        SupplierType type,
        @Nullable String contactName,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String address,
        @Nullable String notes) {}
