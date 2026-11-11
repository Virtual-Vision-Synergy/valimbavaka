package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLocationRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.service.StockDayLedgerService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

/**
 * Reconciles the three points of sale (store, delivery, salon) from the stock movements of the day. The warehouse
 * is not reconciled: it is the source of the morning transfers.
 */
@Service
@Transactional(readOnly = true)
public class DayClosingServiceImpl implements DayClosingService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    /** Locations reconciled, in display order (codes of the stock location referential, SYS-05). */
    static final List<String> LOCATION_CODES = List.of("STORE", "DELIVERY", "TRADE_SHOW");

    static final String DELIVERY_CODE = "DELIVERY";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final PageQuery ALL_LOCATIONS = new PageQuery(1, 100, null, "code", null);

    private final BusinessDayService businessDayService;
    private final ReferentialService referentialService;
    private final StockDayLedgerService stockDayLedgerService;
    private final ArticleService articleService;
    private final Clock clock;

    DayClosingServiceImpl(
            BusinessDayService businessDayService,
            ReferentialService referentialService,
            StockDayLedgerService stockDayLedgerService,
            ArticleService articleService,
            Clock clock) {
        this.businessDayService = businessDayService;
        this.referentialService = referentialService;
        this.stockDayLedgerService = stockDayLedgerService;
        this.articleService = articleService;
        this.clock = clock;
    }

    @Override
    public DayClosingRDto getClosing() {
        var day = businessDayService.findToday();
        if (day == null) {
            var today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
            throw new ConflictException("La journée du %s n'est pas ouverte.".formatted(DATE_FORMAT.format(today)));
        }
        var locations = reconciledLocations();
        var tallies = tallies(day.businessDate(), locations);
        var articles = articleService.getArticlesByIds(tallies.articleIds()).stream()
                .collect(Collectors.toMap(ArticleRDto::id, Function.identity()));
        var blocks = locations.stream()
                .map(location -> block(location, tallies.of(location.id()), articles))
                .toList();
        return new DayClosingRDto(day.businessDate(), day.status(), blocks);
    }

    /** Active locations among the three reconciled ones, in display order. */
    private List<StockLocationRDto> reconciledLocations() {
        var byCode = referentialService.getStockLocations(ALL_LOCATIONS).items().stream()
                .filter(location -> LOCATION_CODES.contains(location.code()))
                .collect(Collectors.toMap(StockLocationRDto::code, Function.identity()));
        return LOCATION_CODES.stream().map(byCode::get).filter(Objects::nonNull).toList();
    }

    /** The day runs from midnight to midnight in Madagascar (the same day as the sales, JR-03). */
    private ClosingTallies tallies(LocalDate date, List<StockLocationRDto> locations) {
        var from = date.atStartOfDay(BUSINESS_ZONE).toInstant();
        var to = date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        var ids = locations.stream().map(StockLocationRDto::id).toList();
        var deliveryIds = locations.stream()
                .filter(location -> DELIVERY_CODE.equals(location.code()))
                .map(StockLocationRDto::id)
                .collect(Collectors.toUnmodifiableSet());
        var tallies = new ClosingTallies(deliveryIds);
        tallies.addOpening(stockDayLedgerService.getBalancesBefore(from, ids));
        tallies.addMovements(stockDayLedgerService.getMovementTotals(from, to, ids));
        tallies.addSystemStock(stockDayLedgerService.getBalancesBefore(to, ids));
        return tallies;
    }

    private static ClosingLocationRDto block(
            StockLocationRDto location, Map<UUID, ClosingTally> tallies, Map<UUID, ArticleRDto> articles) {
        var lines = tallies.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty() && articles.containsKey(entry.getKey()))
                .map(entry -> line(articles.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(ClosingLineRDto::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ClosingLineRDto::reference))
                .toList();
        return new ClosingLocationRDto(location.id(), location.code(), location.label(), lines);
    }

    private static ClosingLineRDto line(ArticleRDto article, ClosingTally tally) {
        return new ClosingLineRDto(
                article.id(),
                article.code(),
                article.name(),
                tally.opening(),
                tally.transferred(),
                tally.returned(),
                tally.sold(),
                tally.delivered(),
                tally.salon(),
                tally.shooting(),
                tally.theoretical(),
                tally.systemStock(),
                tally.gap());
    }
}
