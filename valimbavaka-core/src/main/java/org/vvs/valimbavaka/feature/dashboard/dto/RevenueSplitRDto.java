package org.vvs.valimbavaka.feature.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Split of the turnover of one business day (DSH-04) by product range and by sales channel, best turnover first. Each
 * axis is relative to its own total and its shares add up to exactly 100.0 (all 0.0 when nothing was sold: the lists
 * are then still present, at zero). Fixed-size lists (one entry per active range or channel), not paged.
 */
public record RevenueSplitRDto(LocalDate date, List<RangeShareRDto> byRange, List<ChannelShareRDto> byChannel) {}
