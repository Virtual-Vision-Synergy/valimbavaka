package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingSalonLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingSalonRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingSalonService;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonConsumptionRDto;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;

/**
 * Compares the products consumed by the services of the day with the « Salon » column of the closing. The closing is
 * read through {@link DayClosingService}, so the value compared is exactly the one the administrator sees.
 */
@Service
@Transactional(readOnly = true)
public class DayClosingSalonServiceImpl implements DayClosingSalonService {

    private final DayClosingService dayClosingService;
    private final ServiceRecordService serviceRecordService;
    private final ArticleService articleService;

    DayClosingSalonServiceImpl(
            DayClosingService dayClosingService,
            ServiceRecordService serviceRecordService,
            ArticleService articleService) {
        this.dayClosingService = dayClosingService;
        this.serviceRecordService = serviceRecordService;
        this.articleService = articleService;
    }

    @Override
    public ClosingSalonRDto getSalonConsumption() {
        var closing = dayClosingService.getClosing();
        var date = closing.businessDate();
        var from = date.atStartOfDay(DayClosingServiceImpl.BUSINESS_ZONE).toInstant();
        var to = date.plusDays(1)
                .atStartOfDay(DayClosingServiceImpl.BUSINESS_ZONE)
                .toInstant();
        var consumed = serviceRecordService.getSalonConsumption(from, to).stream()
                .collect(Collectors.toMap(SalonConsumptionRDto::articleId, SalonConsumptionRDto::quantity));
        var inClosing = salonColumn(closing);
        var ids = new LinkedHashSet<UUID>(consumed.keySet());
        ids.addAll(inClosing.keySet());
        var articles = articleService.getArticlesByIds(ids).stream()
                .collect(Collectors.toMap(ArticleRDto::id, Function.identity()));
        var lines = ids.stream()
                .filter(articles::containsKey)
                .map(id -> line(articles.get(id), consumed.get(id), inClosing.get(id)))
                .sorted(Comparator.comparing(ClosingSalonLineRDto::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ClosingSalonLineRDto::reference))
                .toList();
        var servicesCount =
                serviceRecordService.getServiceRecordsBetween(from, to).size();
        return new ClosingSalonRDto(
                date,
                servicesCount,
                total(consumed),
                total(inClosing),
                lines.stream().allMatch(line -> line.gap().signum() == 0),
                lines);
    }

    /** The « Salon » column of the closing, added up per article over the reconciled locations. */
    private static Map<UUID, BigDecimal> salonColumn(DayClosingRDto closing) {
        return closing.locations().stream()
                .flatMap(location -> location.lines().stream())
                .filter(line -> line.salon().signum() != 0)
                .collect(Collectors.toMap(ClosingLineRDto::articleId, ClosingLineRDto::salon, BigDecimal::add));
    }

    private static ClosingSalonLineRDto line(ArticleRDto article, BigDecimal consumed, BigDecimal inClosing) {
        var served = consumed == null ? BigDecimal.ZERO : consumed;
        var closed = inClosing == null ? BigDecimal.ZERO : inClosing;
        return new ClosingSalonLineRDto(
                article.id(), article.code(), article.name(), served, closed, closed.subtract(served));
    }

    private static BigDecimal total(Map<UUID, BigDecimal> quantities) {
        return quantities.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
