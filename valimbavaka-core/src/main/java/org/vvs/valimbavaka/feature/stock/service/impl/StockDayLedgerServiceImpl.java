package org.vvs.valimbavaka.feature.stock.service.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.stock.dto.StockBalanceRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementTotalRDto;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;
import org.vvs.valimbavaka.feature.stock.service.StockDayLedgerService;

@Service
@Transactional(readOnly = true)
public class StockDayLedgerServiceImpl implements StockDayLedgerService {

    private final StockMovementRepository stockMovementRepository;

    StockDayLedgerServiceImpl(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    public List<StockMovementTotalRDto> getMovementTotals(Instant from, Instant to, Collection<UUID> locationIds) {
        if (locationIds.isEmpty()) {
            return List.of();
        }
        return stockMovementRepository.totalsByType(from, to, locationIds).stream()
                .map(total -> new StockMovementTotalRDto(
                        total.articleId(), total.locationId(), total.type(), total.quantity()))
                .toList();
    }

    @Override
    public List<StockBalanceRDto> getBalancesBefore(Instant before, Collection<UUID> locationIds) {
        if (locationIds.isEmpty()) {
            return List.of();
        }
        return stockMovementRepository.balancesBefore(before, locationIds).stream()
                .map(balance -> new StockBalanceRDto(balance.articleId(), balance.locationId(), balance.quantity()))
                .toList();
    }
}
