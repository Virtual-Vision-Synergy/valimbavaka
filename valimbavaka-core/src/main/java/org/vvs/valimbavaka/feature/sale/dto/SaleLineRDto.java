package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Sale line with its frozen unit price; the purchase cost is not exposed. */
public record SaleLineRDto(UUID articleId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
