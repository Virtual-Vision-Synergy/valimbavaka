package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductionInputRDto(UUID articleId, BigDecimal quantity) {}
