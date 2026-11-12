package org.vvs.valimbavaka.feature.report.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Bounded period of a report (RPT-01), in business days {@code [from, to]} inclusive: the day, the calendar month or
 * the calendar year that contains a given date. Every report of the milestone is built on it, so that none can run
 * without a bound; {@link #previous()} gives the period to compare with (RPT-02).
 */
public record ReportPeriod(ReportGranularity granularity, LocalDate from, LocalDate to) {

    /** The period of the given granularity that contains {@code date}. */
    public static ReportPeriod containing(ReportGranularity granularity, LocalDate date) {
        return switch (granularity) {
            case DAY -> new ReportPeriod(granularity, date, date);
            case MONTH ->
                new ReportPeriod(
                        granularity,
                        date.withDayOfMonth(1),
                        YearMonth.from(date).atEndOfMonth());
            case YEAR ->
                new ReportPeriod(
                        granularity, date.withDayOfYear(1), date.withMonth(12).withDayOfMonth(31));
        };
    }

    /** The period of the same granularity just before this one (the day, month or year before). */
    public ReportPeriod previous() {
        return containing(granularity, from.minusDays(1));
    }

    /**
     * The points of the evolution curve (RPT-03): every business day of the period, or the first day of each of the
     * twelve months for a year, with no gap.
     */
    public List<LocalDate> points() {
        if (granularity == ReportGranularity.YEAR) {
            return IntStream.range(0, 12).mapToObj(from::plusMonths).toList();
        }
        return from.datesUntil(to.plusDays(1)).toList();
    }

    /** The point of the curve that a business day belongs to: the day itself, or the first day of its month. */
    public LocalDate pointOf(LocalDate day) {
        return granularity == ReportGranularity.YEAR ? day.withDayOfMonth(1) : day;
    }

    /** Whether this period starts after {@code day}, i.e. has not begun yet. */
    public boolean startsAfter(LocalDate day) {
        return from.isAfter(day);
    }
}
