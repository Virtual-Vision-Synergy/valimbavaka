package org.vvs.valimbavaka.feature.day.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.feature.day.domain.DayTransfer;
import org.vvs.valimbavaka.feature.day.dto.DayQuantityRDto;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;

/**
 * Quantities dispatched on the previous working day (JR-02), read from the lines of its morning transfers: they
 * prefill the grid when the administrator takes yesterday's quantities again.
 */
@Component
class PreviousDayQuantities {

    private final TransferService transferService;

    PreviousDayQuantities(TransferService transferService) {
        this.transferService = transferService;
    }

    /** Article id → quantity per destination; empty without previous day or when it had no morning transfer. */
    Map<UUID, List<DayQuantityRDto>> of(@Nullable BusinessDay previousDay) {
        if (previousDay == null || previousDay.getTransfers().isEmpty()) {
            return Map.of();
        }
        var transferIds = previousDay.getTransfers().stream().map(DayTransfer::getTransferId).toList();
        var quantities = new HashMap<UUID, List<DayQuantityRDto>>();
        for (var transfer : transferService.getTransfersByIds(transferIds)) {
            for (var line : transfer.lines()) {
                quantities
                        .computeIfAbsent(line.articleId(), articleId -> new ArrayList<>())
                        .add(new DayQuantityRDto(transfer.toLocationId(), line.quantity()));
            }
        }
        return quantities;
    }
}
