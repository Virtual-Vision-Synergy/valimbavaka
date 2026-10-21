package org.vvs.valimbavaka.feature.supplier.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierRDto;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

public interface SupplierService {

    PageResponse<SupplierRDto> getSuppliers(PageQuery query, @Nullable SupplierType type, @Nullable Boolean active);

    SupplierRDto getSupplierById(UUID supplierId);

    SupplierRDto createSupplier(SupplierWDto request);

    SupplierRDto updateSupplier(UUID supplierId, SupplierWDto request);

    /** Activates or deactivates a supplier (idempotent: no trace if the state is already the requested one). */
    SupplierRDto setActive(UUID supplierId, boolean active);
}
