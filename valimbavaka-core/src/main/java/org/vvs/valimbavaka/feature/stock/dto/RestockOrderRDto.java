package org.vvs.valimbavaka.feature.stock.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.domain.RestockOrderStatus;

/** Restock order (STK-05) with its lines, ordered by {@code lineNumber} (most urgent first). */
public record RestockOrderRDto(
        UUID id, String reference, RestockOrderStatus status, Instant createdAt, List<RestockOrderLineRDto> lines) {}
