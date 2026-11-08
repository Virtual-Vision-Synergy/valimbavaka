package org.vvs.valimbavaka.feature.transfer.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;

/** Row of the transfer history (TRF-01): the transfer without its lines, only their count and total quantity. */
public record TransferSummary(
        UUID id,
        String number,
        TransferOrigin origin,
        TransferStatus status,
        UUID fromLocationId,
        UUID toLocationId,
        Long lineCount,
        BigDecimal quantity,
        Instant createdAt,
        @Nullable UUID createdBy) {}
