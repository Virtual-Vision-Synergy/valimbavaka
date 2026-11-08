package org.vvs.valimbavaka.feature.production.service;

import org.vvs.valimbavaka.feature.production.dto.ProductionIndicatorsRDto;

/** Indicators shown at the top of the production screen (PRD-04). */
public interface ProductionIndicatorService {

    /** Completed batches, units produced and material cost per unit of the current month of Madagascar. */
    ProductionIndicatorsRDto getIndicators();
}
