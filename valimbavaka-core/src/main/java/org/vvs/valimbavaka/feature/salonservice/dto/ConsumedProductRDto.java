package org.vvs.valimbavaka.feature.salonservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Product usually consumed by a salon service; {@code quantity} is in the article's unit, three decimals. */
public record ConsumedProductRDto(UUID articleId, BigDecimal quantity) {}
