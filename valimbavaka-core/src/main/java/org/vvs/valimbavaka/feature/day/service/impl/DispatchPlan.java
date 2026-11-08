package org.vvs.valimbavaka.feature.day.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Checked morning dispatch: the warehouse and, per destination (in the order of the request), the quantity of each
 * article (same article added up).
 */
record DispatchPlan(UUID warehouseId, Map<UUID, Map<UUID, BigDecimal>> byDestination) {}
