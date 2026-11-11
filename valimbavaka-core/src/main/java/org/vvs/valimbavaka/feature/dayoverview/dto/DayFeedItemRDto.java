package org.vvs.valimbavaka.feature.dayoverview.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.dayoverview.domain.DayFeedType;

/**
 * One operation of the day's feed. {@code reference} is the number of the sale, order or service; {@code label} the
 * channel (sale), customer (delivery) or service name; {@code status} the status code of a sale
 * ({@code VALIDATED}, {@code CANCELLED}, {@code REVERSED}) or of a delivery ({@code ON_ROUND}, {@code DELIVERED}…),
 * {@code null} for a service; {@code actorName} the seller when known; {@code occurredAt} the time of the sale, of
 * the last change of the delivery or of the service.
 */
public record DayFeedItemRDto(
        DayFeedType type,
        UUID id,
        String reference,
        @Nullable String label,
        @Nullable BigDecimal amount,
        @Nullable String status,
        @Nullable String actorName,
        Instant occurredAt) {}
