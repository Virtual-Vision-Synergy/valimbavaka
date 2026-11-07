package org.vvs.valimbavaka.feature.transfer.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;

/**
 * Transfer of stock between two locations. {@code createdAt} is the date of the transfer, {@code createdBy} the
 * user who recorded it (null when generated outside an authenticated request).
 */
public record TransferRDto(
        UUID id,
        String number,
        TransferOrigin origin,
        TransferStatus status,
        UUID fromLocationId,
        UUID toLocationId,
        @Nullable String note,
        List<TransferLineRDto> lines,
        Instant createdAt,
        @Nullable UUID createdBy) {}
