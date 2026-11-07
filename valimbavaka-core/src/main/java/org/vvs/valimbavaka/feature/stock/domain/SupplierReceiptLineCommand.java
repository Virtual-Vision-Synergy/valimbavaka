package org.vvs.valimbavaka.feature.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One received article (STK-06): code and name are copied at reception time, the cost is the unit purchase cost.
 *
 * @param quantity positive, 3 decimals at most
 * @param unitCost unit purchase cost in ariary, zero or more
 */
public record SupplierReceiptLineCommand(
        UUID articleId, String articleCode, String articleName, BigDecimal quantity, BigDecimal unitCost) {}
