package org.vvs.valimbavaka.feature.transfer.service;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.dto.StockReturnWDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferSummaryRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Transfers of stock between locations (TRF-03). {@link #record} is the port reused by the other features (morning
 * transfers of JR-01 and TRF-02); the stock moves through {@code StockMovementService}, never directly.
 */
public interface TransferService {

    /**
     * Records a transfer, moves the stock (out of the origin, into the destination, for each line) and traces it in
     * the journal, all or nothing. 422 if the locations are equal, unknown or inactive, if there is no line or more
     * than 100, if an article appears twice or a quantity is not positive (3 decimals at most); 404 if an article is
     * unknown; 409 if the origin does not hold enough stock. Must be called in a transaction.
     */
    TransferRDto record(TransferCommand command);

    /** Records a stock return (origin {@code RETURN}, status {@code RETURN}, number {@code RET-0001}...). */
    TransferRDto recordReturn(StockReturnWDto request);

    /**
     * Transfer history (TRF-01), newest first by default; sortable by {@code createdAt}, {@code number},
     * {@code origin} and {@code status}, searchable on the number, filtered by origin when {@code origin} is set.
     */
    PageResponse<TransferSummaryRDto> getTransfers(PageQuery query, @Nullable TransferOrigin origin);
}
