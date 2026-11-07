package org.vvs.valimbavaka.feature.production.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Content of a batch, already checked by the service (article types, active seller, distinct inputs).
 *
 * @param quantity quantity of the output article, positive, 3 decimals at most
 * @param operatorId seller responsible for the batch
 */
public record ProductionBatchDetails(
        UUID outputArticleId, BigDecimal quantity, UUID operatorId, List<ProductionInput> inputs) {}
