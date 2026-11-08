package org.vvs.valimbavaka.feature.salonservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Total quantity of one article consumed by the salon services over a period (SRV-04, JR-04), positive. */
public record SalonConsumptionRDto(UUID articleId, BigDecimal quantity) {}
