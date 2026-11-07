package org.vvs.valimbavaka.feature.commission.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Commission rules by sales channel (COM-01), administrator only; at most one active rule per channel. */
public interface CommissionRuleService {

    PageResponse<CommissionRuleRDto> getRules(PageQuery query, @Nullable UUID salesChannelId, @Nullable Boolean active);

    CommissionRuleRDto getRuleById(UUID ruleId);

    /**
     * Creates an active rule.
     *
     * @throws org.vvs.valimbavaka.core.exception.ConflictException if the name is taken or the channel already has
     *         an active rule
     * @throws org.vvs.valimbavaka.core.exception.BusinessException if the sales channel is unknown or inactive
     */
    CommissionRuleRDto createRule(CommissionRuleWDto request);

    /** Updates a rule; same checks as {@link #createRule}, plus 409 on a stale version. */
    CommissionRuleRDto updateRule(UUID ruleId, CommissionRuleWDto request);

    /**
     * Activates or deactivates a rule (idempotent: no trace if the state is already the requested one).
     *
     * @throws org.vvs.valimbavaka.core.exception.ConflictException on activation if the channel already has an
     *         active rule
     */
    CommissionRuleRDto setActive(UUID ruleId, boolean active);
}
