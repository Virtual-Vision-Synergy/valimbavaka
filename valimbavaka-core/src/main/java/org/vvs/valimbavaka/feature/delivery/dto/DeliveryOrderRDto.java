package org.vvs.valimbavaka.feature.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryFailureReason;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;

/**
 * Order to deliver. {@code scheduledDate} + {@code driverId} identify the round, {@code stopNumber} the position in
 * it; {@code amount} is the amount to collect; {@code createdBy} is the seller who entered the order. A failed order
 * carries its {@code failureReason}, the driver's {@code failureNote} and the stock return {@code returnTransferId}.
 */
public record DeliveryOrderRDto(
        UUID id,
        String number,
        DeliveryStatus status,
        String customerName,
        String customerPhone,
        String address,
        @Nullable String district,
        UUID driverId,
        LocalDate scheduledDate,
        int stopNumber,
        BigDecimal amount,
        @Nullable String note,
        @Nullable DeliveryFailureReason failureReason,
        @Nullable String failureNote,
        @Nullable UUID returnTransferId,
        List<DeliveryOrderLineRDto> lines,
        Instant createdAt,
        @Nullable UUID createdBy,
        long version) {}
