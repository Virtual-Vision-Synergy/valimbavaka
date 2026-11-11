package org.vvs.valimbavaka.feature.dashboard.dto;

/**
 * Orders scheduled today. {@code total} leaves out the cancelled orders ({@code cancelled}); {@code delivered},
 * {@code onRound}, {@code toPrepare} and {@code failed} are the others, per status.
 */
public record DeliveryKpiRDto(long total, long delivered, long onRound, long toPrepare, long failed, long cancelled) {}
