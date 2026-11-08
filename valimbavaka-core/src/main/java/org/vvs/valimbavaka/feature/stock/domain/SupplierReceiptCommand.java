package org.vvs.valimbavaka.feature.stock.domain;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Checked content of a supplier receipt (STK-06): supplier (name copied), destination location, restock order taken
 * back (STK-05) if any, and the received lines.
 */
public record SupplierReceiptCommand(
        UUID supplierId,
        String supplierName,
        UUID locationId,
        @Nullable UUID restockOrderId,
        @Nullable String note,
        List<SupplierReceiptLineCommand> lines) {}
