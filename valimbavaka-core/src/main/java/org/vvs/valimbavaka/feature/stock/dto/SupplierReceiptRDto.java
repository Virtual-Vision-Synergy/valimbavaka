package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Supplier receipt (STK-06): {@code VALIDATED}, or {@code REVERSED} once corrected by inverse movements. Lines in
 * the order they were entered; {@code totalCost} is the sum of the line costs.
 */
public record SupplierReceiptRDto(
        UUID id,
        String number,
        OperationStatus status,
        UUID supplierId,
        String supplierName,
        UUID locationId,
        @Nullable UUID restockOrderId,
        @Nullable String note,
        Instant createdAt,
        BigDecimal totalCost,
        List<SupplierReceiptLineRDto> lines) {}
