package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** {@code unitCost}: purchase cost frozen at completion (PRD-02), null before or if the article has no cost. */
public record ProductionInputRDto(
        UUID articleId, BigDecimal quantity, @Nullable BigDecimal unitCost) {}
