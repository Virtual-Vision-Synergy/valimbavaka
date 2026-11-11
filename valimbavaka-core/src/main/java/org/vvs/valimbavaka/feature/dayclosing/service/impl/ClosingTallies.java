package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.vvs.valimbavaka.feature.stock.dto.StockBalanceRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementTotalRDto;

/** The tallies of the day by location then article, filled from the three readings of the stock ledger. */
final class ClosingTallies {

    private final Set<UUID> deliveryLocationIds;
    private final Map<UUID, Map<UUID, ClosingTally>> byLocation = new HashMap<>();

    ClosingTallies(Set<UUID> deliveryLocationIds) {
        this.deliveryLocationIds = deliveryLocationIds;
    }

    /** Quantities held when the day started. */
    void addOpening(List<StockBalanceRDto> balances) {
        for (var balance : balances) {
            tally(balance.locationId(), balance.articleId()).setOpening(balance.quantity());
        }
    }

    /** The day's movements, by type. */
    void addMovements(List<StockMovementTotalRDto> totals) {
        for (var total : totals) {
            tally(total.locationId(), total.articleId())
                    .add(total.type(), total.quantity(), deliveryLocationIds.contains(total.locationId()));
        }
    }

    /** Quantities held at the end of the reading, the starting point of the evening count. */
    void addSystemStock(List<StockBalanceRDto> balances) {
        for (var balance : balances) {
            tally(balance.locationId(), balance.articleId()).setSystemStock(balance.quantity());
        }
    }

    /** Tallies of a location by article; empty if nothing happened there. */
    Map<UUID, ClosingTally> of(UUID locationId) {
        return byLocation.getOrDefault(locationId, Map.of());
    }

    Set<UUID> articleIds() {
        var ids = new HashSet<UUID>();
        byLocation.values().forEach(articles -> ids.addAll(articles.keySet()));
        return ids;
    }

    private ClosingTally tally(UUID locationId, UUID articleId) {
        return byLocation
                .computeIfAbsent(locationId, id -> new HashMap<>())
                .computeIfAbsent(articleId, id -> new ClosingTally());
    }
}
