package org.vvs.valimbavaka.feature.inventory.service.impl;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryReason;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryReasonWDto;
import org.vvs.valimbavaka.feature.inventory.mapper.InventoryMapper;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.inventory.service.InventoryReasonService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

@Service
@Transactional(readOnly = true)
public class InventoryReasonServiceImpl implements InventoryReasonService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReasonServiceImpl.class);

    private static final String RESOURCE_TYPE = "INVENTORY_SESSION";

    private final InventorySessionRepository sessionRepository;
    private final InventoryLineRepository lineRepository;
    private final InventoryMapper inventoryMapper;
    private final ArticleService articleService;
    private final JournalService journalService;

    public InventoryReasonServiceImpl(
            InventorySessionRepository sessionRepository,
            InventoryLineRepository lineRepository,
            InventoryMapper inventoryMapper,
            ArticleService articleService,
            JournalService journalService) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.inventoryMapper = inventoryMapper;
        this.articleService = articleService;
        this.journalService = journalService;
    }

    @Override
    @Transactional
    public InventoryLineRDto qualifyLine(UUID sessionId, UUID lineId, InventoryReasonWDto request) {
        var session = sessionInProgress(sessionId);
        var line = lineRepository
                .findByIdAndSessionId(lineId, sessionId)
                .orElseThrow(() -> new NotFoundException("Ligne d'inventaire introuvable : " + lineId));
        if (!line.hasGap()) {
            throw new BusinessException(
                    "La ligne " + line.getArticleCode() + " n'est pas en écart : aucun motif à qualifier.");
        }
        var reason = request.reason();
        if (line.qualify(reason)) {
            lineRepository.save(line);
            journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, sessionId, describe(session, line, reason));
            log.info("Motif {} donné à la ligne {} de la session d'inventaire {}", reason, lineId, sessionId);
        }
        var articleId = line.getArticleId();
        var unitCost = articleService.getCosts(Set.of(articleId)).get(articleId);
        var expected = Objects.requireNonNullElse(line.getExpectedQuantity(), BigDecimal.ZERO);
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

    /** Example: "Motif d'écart de ART-0031 dans INV-0012 : Casse". */
    private static String describe(InventorySession session, InventoryLine line, InventoryReason reason) {
        return "Motif d'écart de %s dans %s : %s".formatted(line.getArticleCode(), session.getNumber(), label(reason));
    }

    /** French label of the reason, as displayed on the counting page. */
    private static String label(InventoryReason reason) {
        return switch (reason) {
            case BREAKAGE -> "Casse";
            case LOSS -> "Perte ou vol";
            case ENTRY_ERROR -> "Erreur de saisie";
            case UNDECLARED_SHOOTING -> "Shooting non déclaré";
            case OTHER -> "Autre motif";
        };
    }
}
