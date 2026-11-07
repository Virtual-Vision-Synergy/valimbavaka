package org.vvs.valimbavaka.feature.inventory.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionWDto;
import org.vvs.valimbavaka.feature.inventory.mapper.InventoryMapper;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.inventory.service.InventoryService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private static final String RESOURCE_TYPE = "INVENTORY_SESSION";
    private static final PageQuery ALL_LOCATIONS = new PageQuery(1, 100, null, "code", null);
    private static final Set<String> SESSION_SORT_FIELDS = Set.of("createdAt", "number", "countDate");
    private static final Set<String> LINE_SORT_FIELDS = Set.of("articleCode", "articleName");

    private final InventorySessionRepository sessionRepository;
    private final InventoryLineRepository lineRepository;
    private final InventoryOpeningPolicy openingPolicy;
    private final InventoryScopeResolver scopeResolver;
    private final InventoryMapper inventoryMapper;
    private final ReferentialService referentialService;
    private final StockService stockService;
    private final ArticleService articleService;
    private final JournalService journalService;

    InventoryServiceImpl(
            InventorySessionRepository sessionRepository,
            InventoryLineRepository lineRepository,
            InventoryOpeningPolicy openingPolicy,
            InventoryScopeResolver scopeResolver,
            InventoryMapper inventoryMapper,
            ReferentialService referentialService,
            StockService stockService,
            ArticleService articleService,
            JournalService journalService) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.openingPolicy = openingPolicy;
        this.scopeResolver = scopeResolver;
        this.inventoryMapper = inventoryMapper;
        this.referentialService = referentialService;
        this.stockService = stockService;
        this.articleService = articleService;
        this.journalService = journalService;
    }

    @Override
    public PageResponse<InventorySessionRDto> getSessions(
            PageQuery query, @Nullable InventoryStatus status, @Nullable UUID locationId) {
        var pageable = query.toPageable(SESSION_SORT_FIELDS, "createdAt", SortDirection.DESC);
        var locations = locationsById();
        return PageResponse.from(
                sessionRepository.search(status, locationId, pageable).map(session -> toRDto(session, locations)));
    }

    @Override
    public InventorySessionRDto getSession(UUID sessionId) {
        return toRDto(findSession(sessionId), locationsById());
    }

    @Override
    public PageResponse<InventoryLineRDto> getLines(UUID sessionId, PageQuery query) {
        var session = findSession(sessionId);
        var pageable = query.toPageable(LINE_SORT_FIELDS, "articleCode", SortDirection.ASC);
        var lines = lineRepository.findBySessionId(sessionId, pageable);
        var articleIds = lines.map(InventoryLine::getArticleId).toSet();
        var quantities = stockService.getQuantities(session.getLocationId(), articleIds);
        var costs = articleIds.isEmpty() ? Map.<UUID, BigDecimal>of() : articleService.getCosts(articleIds);
        return PageResponse.from(lines.map(line -> inventoryMapper.toRDto(
                line, quantities.getOrDefault(line.getArticleId(), BigDecimal.ZERO), costs.get(line.getArticleId()))));
    }

    @Override
    @Transactional
    public InventorySessionRDto openSession(InventorySessionWDto request) {
        var opening = openingPolicy.check(request);
        var articles = scopeResolver.articlesToCount(opening);
        if (articles.isEmpty()) {
            throw new BusinessException("Aucun article à compter dans ce périmètre.");
        }
        var number = "INV-%04d".formatted(sessionRepository.nextNumber());
        var session = sessionRepository.save(InventorySession.open(number, opening));
        lineRepository.saveAll(articles.stream()
                .map(article -> InventoryLine.toCount(session.getId(), article))
                .toList());
        var location = locationOf(session, locationsById());
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                session.getId(),
                "Ouverture de la session d'inventaire %s · %s (%d articles)"
                        .formatted(number, location.label(), articles.size()));
        log.info("Session d'inventaire {} ouverte ({} lignes)", session.getId(), articles.size());
        return inventoryMapper.toRDto(session, location, articles.size());
    }

    private InventorySession findSession(UUID sessionId) {
        return sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session d'inventaire introuvable : " + sessionId));
    }

    private InventorySessionRDto toRDto(InventorySession session, Map<UUID, StockLocationRDto> locations) {
        return inventoryMapper.toRDto(
                session, locationOf(session, locations), lineRepository.countBySessionId(session.getId()));
    }

    /** A location deactivated since the opening is no longer listed by the referential: shown as such. */
    private static StockLocationRDto locationOf(InventorySession session, Map<UUID, StockLocationRDto> locations) {
        var location = locations.get(session.getLocationId());
        return location != null
                ? location
                : new StockLocationRDto(session.getLocationId(), "INACTIVE", "Emplacement désactivé");
    }

    private Map<UUID, StockLocationRDto> locationsById() {
        return referentialService.getStockLocations(ALL_LOCATIONS).items().stream()
                .collect(Collectors.toMap(StockLocationRDto::id, Function.identity()));
    }
}
