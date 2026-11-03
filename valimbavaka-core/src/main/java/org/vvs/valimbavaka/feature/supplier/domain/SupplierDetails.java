package org.vvs.valimbavaka.feature.supplier.domain;

import org.jspecify.annotations.Nullable;

/**
 * Editable supplier sheet data, grouped to respect the parameter limit. A {@code null} lead time means "default of
 * the type" (FRS-04).
 */
public record SupplierDetails(
        String name,
        SupplierType type,
        @Nullable String contactName,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String address,
        @Nullable String notes,
        @Nullable Integer leadTimeDays) {}
