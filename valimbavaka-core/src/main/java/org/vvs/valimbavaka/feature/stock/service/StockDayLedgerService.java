package org.vvs.valimbavaka.feature.stock.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.dto.StockBalanceRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementTotalRDto;

/**
 * Read side of the movements over a period (JR-04: closing reconciliation). Everything is computed from the
 * movements; nothing is written.
 */
public interface StockDayLedgerService {

    /**
     * Movements recorded in {@code [from, to[} in the given locations, summed by article, location and type
     * (signed: an exit is negative). An empty collection of locations gives an empty list.
     */
    List<StockMovementTotalRDto> getMovementTotals(Instant from, Instant to, Collection<UUID> locationIds);

    /**
     * Non-zero quantities held in the given locations by the movements recorded strictly before {@code before},
     * one row per article and location. An empty collection of locations gives an empty list.
     */
    List<StockBalanceRDto> getBalancesBefore(Instant before, Collection<UUID> locationIds);
}
