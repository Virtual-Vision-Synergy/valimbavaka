package org.vvs.valimbavaka.feature.saledetail.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Detail of a sale on one screen (VTE-09): the sale with the names of its channel, seller and articles, the promotion
 * behind the discount, its invoice and its commission. {@code salesChannelLabel} is {@code null} if the channel was
 * deactivated since; {@code sellerId} and {@code sellerName} are {@code null} for a sale credited to nobody;
 * {@code promotion}, {@code invoice} and {@code commission} are {@code null} when there is none.
 */
public record SaleDetailRDto(
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
        @Nullable SaleDetailPromotionRDto promotion,
        List<SaleDetailLineRDto> lines,
        @Nullable SaleDetailInvoiceRDto invoice,
        @Nullable SaleDetailCommissionRDto commission) {}
