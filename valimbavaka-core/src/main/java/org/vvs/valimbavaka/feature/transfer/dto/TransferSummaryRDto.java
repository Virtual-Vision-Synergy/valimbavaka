package org.vvs.valimbavaka.feature.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;

/**
 * Row of the transfer history (TRF-01): reference, date, route, origin, status and issuer. The location labels come
 * from the active stock locations (null for a location deactivated since); {@code createdByName} is the full name of
 * the issuer, {@code Système} for a transfer generated outside a request, {@code Compte inconnu} for a removed
 * account.
 */
public record TransferSummaryRDto(
        UUID id,
        String number,
        TransferOrigin origin,
        TransferStatus status,
        UUID fromLocationId,
        @Nullable String fromLocationLabel,
        UUID toLocationId,
        @Nullable String toLocationLabel,
        int lineCount,
        BigDecimal quantity,
        Instant createdAt,
        @Nullable UUID createdBy,
        String createdByName) {}
