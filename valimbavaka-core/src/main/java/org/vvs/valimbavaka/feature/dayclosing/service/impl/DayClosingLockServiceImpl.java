package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayclosing.domain.CountResult;
import org.vvs.valimbavaka.feature.dayclosing.domain.DayClosingCount;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.repository.DayClosingCountRepository;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingLockService;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

/** Locks today's closing on the saved counts and closes the business day (JR-06). */
@Service
@Transactional(readOnly = true)
public class DayClosingLockServiceImpl implements DayClosingLockService {

    /** Same resource type as the day itself (JR-01, JR-05): the journal of the day shows its closing. */
    static final String RESOURCE_TYPE = "BUSINESS_DAY";

    private static final int LISTED_LIMIT = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BusinessDayService businessDayService;
    private final DayClosingService dayClosingService;
    private final DayClosingCountRepository countRepository;
    private final JournalService journalService;
    private final Clock clock;

    DayClosingLockServiceImpl(
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
    public DayClosingRDto lock() {
        // The day is locked first: a count saved at the same time (JR-05) waits for the end of the closing.
        businessDayService.lockOpenDay();
        var closing = dayClosingService.getClosing();
        var frozen = freeze(closing);
        countRepository.saveAll(frozen.changed());
        businessDayService.closeDay();
        journalService.record(
                JournalAction.UPDATE, RESOURCE_TYPE, closing.businessDayId(), description(closing, frozen));
        return dayClosingService.getClosing();
    }

    /**
     * Matches every line of the reconciliation with its saved count: a missing count, then a gap without reason
     * (the gap is read again on today's theoretical quantity), refuse the lock (422). The counts whose theoretical
     * quantity moved since they were saved are replaced; a reason without gap anymore is cleared.
     */
    private Frozen freeze(DayClosingRDto closing) {
        var saved = savedCounts(closing.businessDayId());
        var now = clock.instant();
        var lines = new ArrayList<Line>();
        var notCounted = new ArrayList<String>();
        for (var location : closing.locations()) {
            for (var line : location.lines()) {
                var count = saved.get(new LineKey(location.locationId(), line.articleId()));
                if (count == null) {
                    notCounted.add(line.reference());
                } else {
                    lines.add(Line.of(count, line, now));
                }
            }
        }
        refuse("Toutes les lignes doivent être comptées avant le verrouillage : ", notCounted);
        var withoutReason = new ArrayList<String>();
        var changed = new ArrayList<DayClosingCount>();
        for (var line : lines) {
            if (line.lacksReason()) {
                withoutReason.add(line.line().reference());
            } else if (line.count().recount(line.result())) {
                changed.add(line.count());
            }
        }
        refuse("Un motif est obligatoire pour chaque écart : ", withoutReason);
        return new Frozen(lines, changed);
    }

    private static void refuse(String message, List<String> references) {
        if (!references.isEmpty()) {
            throw new BusinessException(message + listed(references) + ".");
        }
    }

    private Map<LineKey, DayClosingCount> savedCounts(UUID dayId) {
        var saved = new HashMap<LineKey, DayClosingCount>();
        for (var count : countRepository.findByBusinessDayId(dayId)) {
            saved.put(new LineKey(count.getLocationId(), count.getArticleId()), count);
        }
        return saved;
    }

    /** E.g. {@code Clôture de la journée du 11/11/2026 : 12 ligne(s) comptée(s), 1 écart(s) (ART-0031 -2 LOSS)}. */
    private static String description(DayClosingRDto closing, Frozen frozen) {
        var gaps = new ArrayList<String>();
        for (var line : frozen.lines()) {
            if (line.hasGap()) {
                gaps.add(line.gapLabel());
            }
        }
        var text = "Clôture de la journée du %s : %d ligne(s) comptée(s), %d écart(s)"
                .formatted(DATE_FORMAT.format(closing.businessDate()), frozen.lines().size(), gaps.size());
        return gaps.isEmpty() ? text : text + " (" + listed(gaps) + ")";
    }

    private static String listed(List<String> items) {
        if (items.size() <= LISTED_LIMIT) {
            return String.join(", ", items);
        }
        var others = items.size() - LISTED_LIMIT;
        return String.join(", ", items.subList(0, LISTED_LIMIT)) + " et " + others + " autre(s)";
    }

    private record LineKey(UUID locationId, UUID articleId) {}

    private record Line(DayClosingCount count, ClosingLineRDto line, CountResult result) {

        /** The saved count read again on today's theoretical quantity of the line. */
        static Line of(DayClosingCount count, ClosingLineRDto line, Instant now) {
            var result = new CountResult(count.getCountedQuantity(), line.theoretical(), count.getReason(), now);
            return new Line(count, line, result);
        }

        boolean hasGap() {
            return result.hasGap();
        }

        boolean lacksReason() {
            return result.hasGap() && result.reason() == null;
        }

        /** E.g. {@code ART-0031 -2 LOSS}: reference, counted minus theoretical, reason. */
        String gapLabel() {
            var gap = result.counted().subtract(result.theoretical());
            return "%s %s %s".formatted(line.reference(), signed(gap), result.reason());
        }

        private static String signed(BigDecimal gap) {
            var text = gap.stripTrailingZeros().toPlainString();
            return gap.signum() > 0 ? "+" + text : text;
        }
    }

    private record Frozen(List<Line> lines, List<DayClosingCount> changed) {}
}
