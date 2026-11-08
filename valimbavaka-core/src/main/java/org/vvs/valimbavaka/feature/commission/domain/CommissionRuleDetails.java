package org.vvs.valimbavaka.feature.commission.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Editable fields of a commission rule: name, sales channel (referenced by id), rate in percent and base. */
public record CommissionRuleDetails(String name, UUID salesChannelId, BigDecimal rate, CommissionBase base) {}
