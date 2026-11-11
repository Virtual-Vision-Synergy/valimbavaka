package org.vvs.valimbavaka.feature.saledetail.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Line of a sale with its article; {@code articleCode} and {@code articleName} are {@code null} if it is gone. */
public record SaleDetailLineRDto(
        UUID articleId,
        @Nullable String articleCode,
        @Nullable String articleName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {}
