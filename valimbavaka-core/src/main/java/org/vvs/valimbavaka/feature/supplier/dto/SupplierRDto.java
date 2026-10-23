package org.vvs.valimbavaka.feature.supplier.dto;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;

/** Read DTO for the supplier sheet. */
public record SupplierRDto(
        UUID id,
        String name,
        SupplierType type,
        @Nullable String contactName,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String address,
        @Nullable String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
