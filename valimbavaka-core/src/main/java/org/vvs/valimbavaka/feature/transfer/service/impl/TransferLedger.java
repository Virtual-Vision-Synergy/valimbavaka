package org.vvs.valimbavaka.feature.transfer.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLine;

/**
 * Moves the stock of a recorded transfer (two {@code TRANSFER} movements per line, referencing the transfer) and
 * traces the transfer in the journal, in the caller's transaction.
 */
@Component
class TransferLedger {

    static final String RESOURCE_TYPE = "TRANSFER";

    private final StockMovementService stockMovementService;
    private final JournalService journalService;

    TransferLedger(StockMovementService stockMovementService, JournalService journalService) {
        this.stockMovementService = stockMovementService;
        this.journalService = journalService;
    }

    void post(Transfer transfer, String route) {
        var movements = new ArrayList<StockMovementCommand>();
        for (var line : transfer.getLines()) {
            movements.add(movement(
                    line, transfer.getFromLocationId(), line.getQuantity().negate(), transfer));
            movements.add(movement(line, transfer.getToLocationId(), line.getQuantity(), transfer));
        }
        stockMovementService.recordAll(movements);
        var units =
                transfer.getLines().stream().map(TransferLine::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                transfer.getId(),
                "%s %s · %s : %d article(s), %s unité(s)"
                        .formatted(
                                transfer.getOrigin().label(),
                                transfer.getNumber(),
                                route,
                                transfer.getLines().size(),
                                units.stripTrailingZeros().toPlainString()));
    }

    private static StockMovementCommand movement(
            TransferLine line, UUID locationId, BigDecimal quantity, Transfer transfer) {
        return new StockMovementCommand(
                line.getArticleId(), locationId, StockMovementType.TRANSFER, quantity, transfer.getId());
    }
}
