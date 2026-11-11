package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Row of the day's sales list (VTE-07): reference, time ({@code createdAt}), channel, seller, articles, payment, promo
 * ({@code discountAmount}) and total. The channel label is {@code null} for a channel deactivated since; the seller
 * name is {@code null} for a sale credited to nobody (LIV-03).
 */
public record SaleSummaryRDto(
        UUID id,
        String number,
        LocalDate businessDate,
        Instant createdAt,
        UUID salesChannelId,
        @Nullable String salesChannelLabel,
        @Nullable UUID sellerId,
        @Nullable String sellerName,
        PaymentMethod paymentMethod,
        OperationStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        List<SaleSummaryLineRDto> lines) {}
