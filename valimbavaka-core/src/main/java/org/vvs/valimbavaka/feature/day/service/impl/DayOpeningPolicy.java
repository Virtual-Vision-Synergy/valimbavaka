package org.vvs.valimbavaka.feature.day.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningWDto;
import org.vvs.valimbavaka.feature.day.dto.DispatchLineWDto;
import org.vvs.valimbavaka.feature.day.repository.BusinessDayRepository;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

/**
 * Rules of the morning dispatch checked before anything is written: today not opened yet, destinations active and
 * other than the warehouse, enough stock at the warehouse for the total asked of each article (all destinations).
 */
@Component
class DayOpeningPolicy {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");
    static final String WAREHOUSE_CODE = "WAREHOUSE";
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BusinessDayRepository dayRepository;
    private final ReferentialService referentialService;
    private final StockService stockService;
    private final ArticleService articleService;
    private final Clock clock;

    DayOpeningPolicy(
            BusinessDayRepository dayRepository,
            ReferentialService referentialService,
            StockService stockService,
            ArticleService articleService,
            Clock clock) {
        this.dayRepository = dayRepository;
        this.referentialService = referentialService;
        this.stockService = stockService;
        this.articleService = articleService;
        this.clock = clock;
    }

    /** Today in the business time zone: the day changes at midnight in Antananarivo, not in UTC. */
    LocalDate today() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }

    /** @return the dispatch grouped per destination, ready to be recorded as one transfer per destination */
    DispatchPlan check(LocalDate date, DayOpeningWDto request) {
        if (dayRepository.existsByBusinessDate(date)) {
            throw new ConflictException("La journée du %s est déjà ouverte.".formatted(DATE_FORMAT.format(date)));
        }
        var warehouseId = referentialService.requireActiveStockLocationId(WAREHOUSE_CODE);
        var byDestination = group(request.lines());
        for (var destinationId : byDestination.keySet()) {
            if (destinationId.equals(warehouseId) || !referentialService.isActiveStockLocation(destinationId)) {
                throw new BusinessException("Chaque destination doit être un emplacement actif hors entrepôt.");
            }
        }
        checkWarehouseStock(warehouseId, byDestination);
        return new DispatchPlan(warehouseId, byDestination);
    }

    private static Map<UUID, Map<UUID, BigDecimal>> group(List<DispatchLineWDto> lines) {
        var byDestination = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        for (var line : lines) {
            byDestination
                    .computeIfAbsent(line.destinationId(), destination -> new LinkedHashMap<>())
                    .merge(line.articleId(), line.quantity(), BigDecimal::add);
        }
        return byDestination;
    }

    /** 409 naming the first article asked beyond the warehouse stock; 404 if that article is unknown. */
    private void checkWarehouseStock(UUID warehouseId, Map<UUID, Map<UUID, BigDecimal>> byDestination) {
        var asked = new HashMap<UUID, BigDecimal>();
        for (var quantities : byDestination.values()) {
            quantities.forEach((articleId, quantity) -> asked.merge(articleId, quantity, BigDecimal::add));
        }
        if (asked.isEmpty()) {
            return;
        }
        var available = stockService.getQuantities(warehouseId, asked.keySet());
        for (var entry : asked.entrySet()) {
            var stock = available.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            if (entry.getValue().compareTo(stock) > 0) {
                var name = articleService.getArticleById(entry.getKey()).name();
                var message = "Stock insuffisant à l'entrepôt pour %s : %s demandé(s), %s disponible(s).";
                throw new ConflictException(message.formatted(name, plain(entry.getValue()), plain(stock)));
            }
        }
    }

    private static String plain(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }
}
