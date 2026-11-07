package org.vvs.valimbavaka.feature.inventory.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryCountWDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.mapper.InventoryMapper;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.inventory.service.InventoryCountService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

@Service
@Transactional(readOnly = true)
public class InventoryCountServiceImpl implements InventoryCountService {

    private static final Logger log = LoggerFactory.getLogger(InventoryCountServiceImpl.class);

    private static final String RESOURCE_TYPE = "INVENTORY_SESSION";

    private final InventorySessionRepository sessionRepository;
    private final InventoryLineRepository lineRepository;
    private final InventoryMapper inventoryMapper;
    private final StockService stockService;
    private final ArticleService articleService;
    private final JournalService journalService;
    private final Clock clock;

    public InventoryCountServiceImpl(
            InventorySessionRepository sessionRepository,
            InventoryLineRepository lineRepository,
            InventoryMapper inventoryMapper,
            StockService stockService,
            ArticleService articleService,
            JournalService journalService,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.inventoryMapper = inventoryMapper;
        this.stockService = stockService;
        this.articleService = articleService;
        this.journalService = journalService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public InventoryLineRDto countLine(UUID sessionId, UUID lineId, InventoryCountWDto request) {
        var session = sessionInProgress(sessionId);
        var line = lineRepository
                .findByIdAndSessionId(lineId, sessionId)
                .orElseThrow(() -> new NotFoundException("Ligne d'inventaire introuvable : " + lineId));
        var articleId = line.getArticleId();
        var expected = stockService
                .getQuantities(session.getLocationId(), Set.of(articleId))
                .getOrDefault(articleId, BigDecimal.ZERO);
        if (line.count(request.countedQuantity(), expected, clock.instant())) {
            lineRepository.save(line);
            journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, sessionId, describe(session, line));
            log.info("Ligne {} de la session d'inventaire {} comptée", lineId, sessionId);
        }
        var unitCost = articleService.getCosts(Set.of(articleId)).get(articleId);
        return inventoryMapper.toRDto(line, expected, unitCost);
    }

    private InventorySession sessionInProgress(UUID sessionId) {
        var session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session d'inventaire introuvable : " + sessionId));
        if (session.getStatus() != InventoryStatus.IN_PROGRESS) {
            throw new ConflictException("La session " + session.getNumber() + " n'est plus en cours.");
        }
        return session;
    }

    /** Example: "Comptage de ART-0031 dans INV-0012 : 10 compté(s), 12 attendu(s), écart -2". */
    private static String describe(InventorySession session, InventoryLine line) {
        var counted = requireValue(line.getCountedQuantity());
        var expected = requireValue(line.getExpectedQuantity());
        var gap = counted.subtract(expected);
        var article = line.getArticleCode();
        return "Comptage de %s dans %s : %s compté(s), %s attendu(s), écart %s"
                .formatted(article, session.getNumber(), plain(counted), plain(expected), plain(gap));
    }

    private static BigDecimal requireValue(@Nullable BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
