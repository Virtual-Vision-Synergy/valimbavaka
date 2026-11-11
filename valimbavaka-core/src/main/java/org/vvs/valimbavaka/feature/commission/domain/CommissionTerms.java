package org.vvs.valimbavaka.feature.commission.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Rate given by the COM-01 resolver for an operation: rate in percent, base it applies to, level it comes from and,
 * for a channel rule, the rule's identifier.
 */
public record CommissionTerms(
        BigDecimal rate,
        CommissionBase base,
        CommissionRateSource rateSource,
        @Nullable UUID ruleId) {}
