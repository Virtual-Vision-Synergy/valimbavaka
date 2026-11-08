package org.vvs.valimbavaka.feature.day.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Quantity of an article sent from the warehouse to one destination (stock location) by a morning transfer. */
public record DayQuantityRDto(UUID locationId, BigDecimal quantity) {}
