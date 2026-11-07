package org.vvs.valimbavaka.feature.production.service.impl;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchDetails;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchWDto;
import org.vvs.valimbavaka.feature.production.mapper.ProductionMapper;
import org.vvs.valimbavaka.feature.production.repository.ProductionBatchRepository;
import org.vvs.valimbavaka.feature.production.service.ProductionBatchService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class ProductionBatchServiceImpl implements ProductionBatchService {

    static final String RESOURCE_TYPE = "PRODUCTION_BATCH";

    private static final Logger log = LoggerFactory.getLogger(ProductionBatchServiceImpl.class);

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "number", "status");

    private final ProductionBatchRepository productionBatchRepository;
    private final ProductionMapper productionMapper;
    private final ProductionBatchPolicy policy;
    private final JournalService journalService;
    private final ProductionCompletion completion;

    ProductionBatchServiceImpl(
            ProductionBatchRepository productionBatchRepository,
            ProductionMapper productionMapper,
            ProductionBatchPolicy policy,
            JournalService journalService,
            ProductionCompletion completion) {
        this.productionBatchRepository = productionBatchRepository;
        this.productionMapper = productionMapper;
        this.policy = policy;
        this.journalService = journalService;
        this.completion = completion;
    }

    @Override
    public PageResponse<ProductionBatchRDto> getBatches(PageQuery query, @Nullable ProductionBatchStatus status) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(productionBatchRepository
                .search(query.searchTerm(), status, pageable)
                .map(productionMapper::toRDto));
    }

    @Override
    public ProductionBatchRDto getBatchById(UUID batchId) {
        return productionMapper.toRDto(
                productionBatchRepository.findById(batchId).orElseThrow(() -> notFound(batchId)));
    }

    @Override
    @Transactional
    public ProductionBatchRDto createBatch(ProductionBatchWDto request) {
        return productionMapper.toRDto(plan(request));
    }

    @Override
    @Transactional
    public ProductionBatchRDto startBatch(UUID batchId) {
        var batch = productionBatchRepository.findByIdForUpdate(batchId).orElseThrow(() -> notFound(batchId));
        batch.start();
        var saved = productionBatchRepository.saveAndFlush(batch);
        journalService.record(
                JournalAction.UPDATE,
                RESOURCE_TYPE,
                saved.getId(),
                "Démarrage du lot %s : planifié → en cours".formatted(saved.getNumber()));
        log.info("Lot de production {} démarré", saved.getId());
        return productionMapper.toRDto(saved);
    }

    @Override
    @Transactional
    public ProductionBatchRDto completeBatch(UUID batchId) {
        var batch = productionBatchRepository.findByIdForUpdate(batchId).orElseThrow(() -> notFound(batchId));
        return productionMapper.toRDto(complete(batch));
    }

    @Override
    @Transactional
    public ProductionBatchRDto createCompletedBatch(ProductionBatchWDto request) {
        return productionMapper.toRDto(complete(plan(request)));
    }

    /** Moves the stock, then traces the completion; any refusal rolls back the whole transaction. */
    private ProductionBatch complete(ProductionBatch batch) {
        completion.complete(batch);
        var saved = productionBatchRepository.saveAndFlush(batch);
        journalService.record(
                JournalAction.UPDATE,
                RESOURCE_TYPE,
                saved.getId(),
                "Validation du lot %s : %s entrant(s) sortis, %s unité(s) entrées à l'entrepôt"
                        .formatted(
                                saved.getNumber(),
                                saved.getInputs().size(),
                                saved.getQuantity().stripTrailingZeros().toPlainString()));
        log.info("Lot de production {} validé", saved.getId());
        return saved;
    }

    private ProductionBatch plan(ProductionBatchWDto request) {
        var output = policy.check(request);
        var number = "LOT-%05d".formatted(productionBatchRepository.nextNumber());
        var saved = productionBatchRepository.save(ProductionBatch.plan(number, toDetails(request)));
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                saved.getId(),
                "Création du lot %s : %s × %s · %s (%d entrant(s))"
                        .formatted(
                                number,
                                request.quantity().toPlainString(),
                                output.code(),
                                output.name(),
                                request.inputs().size()));
        log.info("Lot de production {} créé", saved.getId());
        return saved;
    }

    private static NotFoundException notFound(UUID batchId) {
        return new NotFoundException("Lot de production introuvable : " + batchId);
    }

    private static ProductionBatchDetails toDetails(ProductionBatchWDto request) {
        var inputs = request.inputs().stream()
                .map(input -> new ProductionInput(input.articleId(), input.quantity()))
                .toList();
        return new ProductionBatchDetails(request.outputArticleId(), request.quantity(), request.operatorId(), inputs);
    }
}
