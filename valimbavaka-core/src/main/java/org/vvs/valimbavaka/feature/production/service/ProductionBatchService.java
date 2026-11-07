package org.vvs.valimbavaka.feature.production.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Production batches (PRD-01). Status changes (PRD-03) and stock movements (PRD-02) are added here. */
public interface ProductionBatchService {

    /** Most recent first by default; search on the number, null status = all. */
    PageResponse<ProductionBatchRDto> getBatches(PageQuery query, @Nullable ProductionBatchStatus status);

    ProductionBatchRDto getBatchById(UUID batchId);

    /**
     * Plans a batch: output must be an active finished product, inputs active raw materials or semi-finished
     * articles (each at most once), the operator an active seller. Traced in the journal; the stock does not move.
     *
     * @throws org.vvs.valimbavaka.core.exception.NotFoundException if an article is unknown
     * @throws org.vvs.valimbavaka.core.exception.BusinessException if a rule above is broken (422)
     */
    ProductionBatchRDto createBatch(ProductionBatchWDto request);
}
