package org.vvs.valimbavaka.feature.inventory.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.inventory.service.InventoryClosingService;
import org.vvs.valimbavaka.feature.inventory.service.InventoryService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

@Service
@Transactional(readOnly = true)
public class InventoryClosingServiceImpl implements InventoryClosingService {

    private static final Logger log = LoggerFactory.getLogger(InventoryClosingServiceImpl.class);

    private static final String RESOURCE_TYPE = "INVENTORY_SESSION";
    private static final int LISTED_CODES = 5;

    private final InventorySessionRepository sessionRepository;
    private final InventoryLineRepository lineRepository;
    private final InventoryService inventoryService;
    private final StockService stockService;
    private final StockMovementService stockMovementService;
    private final JournalService journalService;
    private final Clock clock;

    public InventoryClosingServiceImpl(
            InventorySessionRepository sessionRepository,
            InventoryLineRepository lineRepository,
            InventoryService inventoryService,
            StockService stockService,
            StockMovementService stockMovementService,
            JournalService journalService,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.inventoryService = inventoryService;
        this.stockService = stockService;
        this.stockMovementService = stockMovementService;
        this.journalService = journalService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public InventorySessionRDto closeSession(UUID sessionId) {
        var session = sessionInProgress(sessionId);
        var lines = lineRepository.findBySessionIdOrderByArticleCode(sessionId);
        checkReady(lines);
        var adjustments = adjustStock(session, lines);
        session.close(clock.instant());
        sessionRepository.saveAndFlush(session);
        var closed = inventoryService.getSession(sessionId);
        journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, sessionId, describe(closed, adjustments));
        log.info("Session d'inventaire {} clôturée : {} ajustement(s) de stock", sessionId, adjustments);
        return closed;
    }

    /** Locks the session until the end of the transaction, then checks it is still in progress. */
    private InventorySession sessionInProgress(UUID sessionId) {
        var session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("Session d'inventaire introuvable : " + sessionId));
        if (session.getStatus() != InventoryStatus.IN_PROGRESS) {
            throw new ConflictException("La session " + session.getNumber() + " n'est plus en cours.");
        }
        return session;
    }

    /** Every line counted and every gap explained (INV-03), as the counting page already requires. */
    private static void checkReady(List<InventoryLine> lines) {
        var notCounted = codesOf(lines, line -> line.getCountedQuantity() == null);
        if (!notCounted.isEmpty()) {
            throw new BusinessException("Clôture impossible : article(s) non compté(s) " + notCounted + ".");
        }
        var withoutReason = codesOf(lines, line -> line.hasGap() && line.getReason() == null);
        if (!withoutReason.isEmpty()) {
            throw new BusinessException("Clôture impossible : écart(s) sans motif " + withoutReason + ".");
        }
    }

    /** Example: "ART-0031, ART-0032 (2)"; empty when no line matches. */
    private static String codesOf(List<InventoryLine> lines, Predicate<InventoryLine> filter) {
        var codes = lines.stream().filter(filter).map(InventoryLine::getArticleCode).toList();
        if (codes.isEmpty()) {
            return "";
        }
        var listed = codes.stream().limit(LISTED_CODES).collect(Collectors.joining(", "));
        return "%s%s (%d)".formatted(listed, codes.size() > LISTED_CODES ? "…" : "", codes.size());
    }

    /**
     * One {@code INVENTORY} movement per article whose current stock differs from its count, so that the stock of
     * the location equals the counted quantities (rule of INV-01: counted − current stock, not the frozen gap).
     */
    private int adjustStock(InventorySession session, List<InventoryLine> lines) {
        var articleIds = lines.stream().map(InventoryLine::getArticleId).toList();
        var current = stockService.getQuantities(session.getLocationId(), articleIds);
        var adjustments = 0;
        for (var line : lines) {
            var stock = current.getOrDefault(line.getArticleId(), BigDecimal.ZERO);
            var delta = counted(line).subtract(stock);
            if (delta.signum() != 0) {
                var command = new StockMovementCommand(
                        line.getArticleId(),
                        session.getLocationId(),
                        StockMovementType.INVENTORY,
                        delta,
                        session.getId());
                stockMovementService.record(command);
                adjustments++;
            }
        }
        return adjustments;
    }

    private static BigDecimal counted(InventoryLine line) {
        var counted = line.getCountedQuantity();
        return counted == null ? BigDecimal.ZERO : counted;
    }

    /** Example: "Clôture de la session d'inventaire INV-0012 · Boutique : 12 article(s), 3 ajustement(s) de stock" */
    private static String describe(InventorySessionRDto session, int adjustments) {
        return "Clôture de la session d'inventaire %s · %s : %d article(s), %d ajustement(s) de stock"
                .formatted(session.number(), session.locationLabel(), session.lineCount(), adjustments);
    }
}
