package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayclosing.domain.CountResult;
import org.vvs.valimbavaka.feature.dayclosing.domain.DayClosingCount;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingCountWDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingCountsWDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.repository.DayClosingCountRepository;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingCountService;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

/** Saves the evening counts of today's closing and refuses a gap without reason (JR-05). */
@Service
@Transactional(readOnly = true)
public class DayClosingCountServiceImpl implements DayClosingCountService {

    /** Same resource type as the day itself (JR-01): the journal of the day shows its counts. */
    static final String RESOURCE_TYPE = "BUSINESS_DAY";

    private static final int LISTED_LIMIT = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BusinessDayService businessDayService;
    private final DayClosingService dayClosingService;
    private final DayClosingCountRepository countRepository;
    private final JournalService journalService;
    private final Clock clock;

    DayClosingCountServiceImpl(
            BusinessDayService businessDayService,
            DayClosingService dayClosingService,
            DayClosingCountRepository countRepository,
            JournalService journalService,
            Clock clock) {
        this.businessDayService = businessDayService;
        this.dayClosingService = dayClosingService;
        this.countRepository = countRepository;
        this.journalService = journalService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DayClosingRDto saveCounts(DayClosingCountsWDto request) {
        // The day is locked first: a closing running at the same time (JR-06) finishes before this count is read.
        businessDayService.lockOpenDay();
        var closing = dayClosingService.getClosing();
        if (closing.status() != DayStatus.OPEN) {
            throw new ConflictException(
                    "La journée du %s est déjà clôturée.".formatted(DATE_FORMAT.format(closing.businessDate())));
        }
        var entries = resolve(closing, request.counts());
        requireReasons(entries.values());
        var saved = persist(closing.businessDayId(), entries);
        if (!saved.isEmpty()) {
            journalService.record(
                    JournalAction.UPDATE, RESOURCE_TYPE, closing.businessDayId(), description(closing, saved));
        }
        return dayClosingService.getClosing();
    }

    /** Matches each count with its line of today's closing (404 if none) and refuses a line sent twice (422). */
    private static Map<LineKey, Entry> resolve(DayClosingRDto closing, List<ClosingCountWDto> counts) {
        var lines = linesByKey(closing);
        var entries = new LinkedHashMap<LineKey, Entry>();
        for (var count : counts) {
            var key = new LineKey(count.locationId(), count.articleId());
            var line = lines.get(key);
            if (line == null) {
                var message = "L'article %s n'a pas de ligne dans la clôture de l'emplacement %s.";
                throw new NotFoundException(message.formatted(count.articleId(), count.locationId()));
            }
            if (entries.put(key, new Entry(count, line)) != null) {
                var message = "Un article ne peut être compté qu'une fois par emplacement : %s.";
                throw new BusinessException(message.formatted(line.reference()));
            }
        }
        return entries;
    }

    private static Map<LineKey, ClosingLineRDto> linesByKey(DayClosingRDto closing) {
        var lines = new HashMap<LineKey, ClosingLineRDto>();
        for (var location : closing.locations()) {
            for (var line : location.lines()) {
                lines.put(new LineKey(location.locationId(), line.articleId()), line);
            }
        }
        return lines;
    }

    /** Every gap needs its reason: the whole save is refused otherwise (422), naming the articles. */
    private static void requireReasons(Collection<Entry> entries) {
        var missing = entries.stream()
                .filter(entry -> entry.hasGap() && entry.count().reason() == null)
                .map(entry -> entry.line().reference())
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("Un motif est obligatoire pour chaque écart : " + listed(missing) + ".");
        }
    }

    /** Creates or replaces the count of each line; returns the lines that changed. */
    private List<DayClosingCount> persist(UUID dayId, Map<LineKey, Entry> entries) {
        var existing = new HashMap<LineKey, DayClosingCount>();
        for (var count : countRepository.findByBusinessDayId(dayId)) {
            existing.put(new LineKey(count.getLocationId(), count.getArticleId()), count);
        }
        var now = clock.instant();
        var changed = new ArrayList<DayClosingCount>();
        for (var mapEntry : entries.entrySet()) {
            var key = mapEntry.getKey();
            var result = mapEntry.getValue().result(now);
            var current = existing.get(key);
            if (current == null) {
                changed.add(DayClosingCount.create(dayId, key.locationId(), key.articleId(), result));
            } else if (current.recount(result)) {
                changed.add(current);
            }
        }
        return countRepository.saveAll(changed);
    }

    /** E.g. {@code Comptage de clôture du 11/11/2026 : 12 ligne(s) enregistrée(s), dont 3 écart(s) motivé(s)}. */
    private static String description(DayClosingRDto closing, List<DayClosingCount> saved) {
        var gaps = saved.stream().filter(DayClosingCount::hasGap).count();
        return "Comptage de clôture du %s : %d ligne(s) enregistrée(s), dont %d écart(s) motivé(s)"
                .formatted(DATE_FORMAT.format(closing.businessDate()), saved.size(), gaps);
    }

    private static String listed(List<String> references) {
        if (references.size() <= LISTED_LIMIT) {
            return String.join(", ", references);
        }
        var others = references.size() - LISTED_LIMIT;
        return String.join(", ", references.subList(0, LISTED_LIMIT)) + " et " + others + " autre(s)";
    }

    private record LineKey(UUID locationId, UUID articleId) {}

    private record Entry(ClosingCountWDto count, ClosingLineRDto line) {

        boolean hasGap() {
            return count.counted().compareTo(line.theoretical()) != 0;
        }

        CountResult result(Instant at) {
            return new CountResult(count.counted(), line.theoretical(), count.reason(), at);
        }
    }
}
