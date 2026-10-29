package org.vvs.valimbavaka.feature.stock.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovement;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementRDto;
import org.vvs.valimbavaka.feature.stock.mapper.StockMovementMapper;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;

@Service
@Transactional(readOnly = true)
public class StockMovementServiceImpl implements StockMovementService {

    static final String RESOURCE_TYPE = "STOCK_MOVEMENT";

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementMapper stockMovementMapper;
    private final JournalService journalService;
    private final StockMovementPolicy policy;

    StockMovementServiceImpl(
            StockMovementRepository stockMovementRepository,
            StockMovementMapper stockMovementMapper,
            JournalService journalService,
            StockMovementPolicy policy) {
        this.stockMovementRepository = stockMovementRepository;
        this.stockMovementMapper = stockMovementMapper;
        this.journalService = journalService;
        this.policy = policy;
    }

    @Override
    @Transactional
    public StockMovementRDto record(StockMovementCommand command) {
        var article = policy.check(command);
        var movement = stockMovementRepository.save(StockMovement.of(command));
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                movement.getId(),
                "Mouvement %s de l'article %s · %s : %s"
                        .formatted(
                                command.type(),
                                article.code(),
                                article.name(),
                                command.quantity().toPlainString()));
        return stockMovementMapper.toRDto(movement);
    }
}
