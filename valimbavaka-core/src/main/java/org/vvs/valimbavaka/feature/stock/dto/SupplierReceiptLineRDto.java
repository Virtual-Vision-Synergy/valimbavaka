package org.vvs.valimbavaka.feature.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Received line (STK-06): code and name as they were at reception time, {@code lineCost} = quantity x unit cost. */
public record SupplierReceiptLineRDto(
        UUID articleId,
        String articleCode,
        String articleName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineCost) {}
