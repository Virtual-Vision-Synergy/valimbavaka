package org.vvs.valimbavaka.feature.transfer.domain;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A transfer requested by a business feature (stock return, morning transfers of the day opening...).
 *
 * @param lines 1 to 100 lines, each article once
 */
public record TransferCommand(
        TransferOrigin origin,
        UUID fromLocationId,
        UUID toLocationId,
        List<TransferLineCommand> lines,
        @Nullable String note) {}
