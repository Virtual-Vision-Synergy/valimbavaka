package org.vvs.valimbavaka.feature.production.service;

import org.vvs.valimbavaka.feature.production.dto.ProductionOperatorRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Sellers that can be chosen as responsible of a batch (PRD-01). */
public interface ProductionOperatorService {

    /** Active sellers, by first name by default; search on first and last name. */
    PageResponse<ProductionOperatorRDto> getOperators(PageQuery query);
}
