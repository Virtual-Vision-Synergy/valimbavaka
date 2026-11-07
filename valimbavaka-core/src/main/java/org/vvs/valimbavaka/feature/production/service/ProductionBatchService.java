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

    /**
     * Completes a planned or in-progress batch (PRD-02) in one transaction: input costs frozen, one outgoing PRODUCTION
     * movement per input and one incoming movement for the output in the warehouse ({@code referenceId} = batch id),
     * status COMPLETED, traced in the journal. Any refused movement rolls back everything.
     *
     * @throws org.vvs.valimbavaka.core.exception.NotFoundException if the batch is unknown
     * @throws org.vvs.valimbavaka.core.exception.ConflictException if already completed or if the stock is short (409)
     * @throws org.vvs.valimbavaka.core.exception.BusinessException if the warehouse is unknown or inactive (422)
     */
    ProductionBatchRDto completeBatch(UUID batchId);

    /**
     * Plans then completes a batch in the same transaction ("Valider le lot" of the screen, also usable offline as a
     * single request): same rules as {@link #createBatch} then {@link #completeBatch}; nothing is kept on refusal.
     */
    ProductionBatchRDto createCompletedBatch(ProductionBatchWDto request);
}
