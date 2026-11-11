package org.vvs.valimbavaka.feature.dayoverview.dto;

/** Orders scheduled today, in total and per status; {@code onRound} is the part of the round still to do. */
public record DayDeliveriesRDto(
        long total, long toPrepare, long onRound, long delivered, long failed, long cancelled) {}
