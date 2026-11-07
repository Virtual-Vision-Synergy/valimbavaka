package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;

/**
 * Rate applicable to a sale (COM-01 resolver, used by COM-02): rate in percent, base it applies to, level it comes
 * from and, for a channel rule, the rule's identifier ({@code null} otherwise).
 */
public record ApplicableCommissionRDto(
        BigDecimal rate, CommissionBase base, CommissionRateSource source, @Nullable UUID ruleId) {}
