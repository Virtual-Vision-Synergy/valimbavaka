package org.vvs.valimbavaka.feature.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Turnover trend of the cockpit (DSH-02): one point per day, from {@code from} to {@code to} (today in Madagascar
 * time, included), oldest first. The series is continuous: a day without sale is a point at 0. It is a fixed-size
 * series (7 points), not a paged list.
 */
public record RevenueTrendRDto(LocalDate from, LocalDate to, List<RevenueTrendPointRDto> points) {}
