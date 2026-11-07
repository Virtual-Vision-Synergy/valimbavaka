package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** Recorded sale; {@code createdAt} is the time of the sale and {@code createdBy} the account that entered it. */
public record SaleRDto(
        UUID id,
        String number,
        UUID businessDayId,
        LocalDate businessDate,
        UUID salesChannelId,
        UUID stockLocationId,
        UUID sellerId,
        PaymentMethod paymentMethod,
        OperationStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        List<SaleLineRDto> lines,
        Instant createdAt,
        @Nullable UUID createdBy) {}
