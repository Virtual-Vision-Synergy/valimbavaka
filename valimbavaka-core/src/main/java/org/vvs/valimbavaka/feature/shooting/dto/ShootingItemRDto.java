package org.vvs.valimbavaka.feature.shooting.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Article taken out, as it was named when the exit was recorded. */
public record ShootingItemRDto(UUID articleId, String articleCode, String articleName, BigDecimal quantity) {}
