package org.vvs.valimbavaka.feature.production.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.production.dto.ProductionBatchCostRDto;

/** Cost price of a production batch, computed from its inputs (PRD-05). */
public interface ProductionCostService {

    /**
     * Inputs of the batch with their unit and line costs, material cost, cost per unit produced and margin on the
     * sale price. Frozen costs once the batch is completed (PRD-02), current purchase costs before ({@code estimated}).
     *
     * @throws org.vvs.valimbavaka.core.exception.NotFoundException if the batch is unknown
     */
    ProductionBatchCostRDto getBatchCost(UUID batchId);
}
