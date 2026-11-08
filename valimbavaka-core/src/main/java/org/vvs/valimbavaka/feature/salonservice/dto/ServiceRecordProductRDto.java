package org.vvs.valimbavaka.feature.salonservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Product consumed by a performed service: code and name copied when recorded, quantity in the article's unit. */
public record ServiceRecordProductRDto(UUID articleId, String articleCode, String articleName, BigDecimal quantity) {}
