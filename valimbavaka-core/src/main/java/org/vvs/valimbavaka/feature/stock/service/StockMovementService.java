package org.vvs.valimbavaka.feature.stock.service;

import java.util.List;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementRDto;

/**
 * The only way to change a stock (STK-01). Business features call it from their own transaction: a transfer
 * records two movements (out of the source, into the destination) and either both are kept or none.
 */
public interface StockMovementService {

    /**
     * Records one typed movement and traces it in the journal. 404 if the article is unknown, 422 if the
     * location is unknown or inactive, if the quantity does not fit the type (zero, wrong sign, more than 3
     * decimals), 409 if the location would end up with a negative stock. Must be called in a transaction.
     */
    StockMovementRDto record(StockMovementCommand command);

    /**
     * Records the movements of one business operation, in order (a transfer: out of the origin, into the
     * destination, for each article). The locks of every article and location are taken first, in a stable order,
     * so that two operations crossing the same locations never deadlock. Same errors as {@link #record}; the
     * first refused movement fails the whole call. Must be called in a transaction.
     */
    List<StockMovementRDto> recordAll(List<StockMovementCommand> commands);
}
