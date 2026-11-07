package org.vvs.valimbavaka.feature.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;

/**
 * Full sheet of a transfer (TRF-04): the row of the history (TRF-01) plus the note and every line, in the order they
 * were entered. {@code quantity} is the sum of the lines; labels and issuer name follow the rules of
 * {@link TransferSummaryRDto}.
 */
public record TransferDetailRDto(
        UUID id,
        String number,
        TransferOrigin origin,
        TransferStatus status,
        UUID fromLocationId,
        @Nullable String fromLocationLabel,
        UUID toLocationId,
        @Nullable String toLocationLabel,
        @Nullable String note,
        List<TransferDetailLineRDto> lines,
        BigDecimal quantity,
        Instant createdAt,
        @Nullable UUID createdBy,
        String createdByName) {}
