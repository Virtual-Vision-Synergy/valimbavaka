package org.vvs.valimbavaka.feature.production.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchCostRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionInputCostRDto;
import org.vvs.valimbavaka.feature.production.repository.ProductionBatchRepository;
import org.vvs.valimbavaka.feature.production.service.ProductionCostService;

/**
 * Cost price of a batch (PRD-05), from its inputs: material cost = sum of {@code quantity x unit cost}, divided by
 * the quantity produced. A completed batch uses the costs frozen at its completion (PRD-02); before, the current
 * purchase costs give an estimate. An input without cost leaves the totals null instead of counting for 0.
 */
@Service
@Transactional(readOnly = true)
public class ProductionCostServiceImpl implements ProductionCostService {

    private static final int COST_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProductionBatchRepository productionBatchRepository;
    private final ArticleService articleService;

    ProductionCostServiceImpl(ProductionBatchRepository productionBatchRepository, ArticleService articleService) {
        this.productionBatchRepository = productionBatchRepository;
        this.articleService = articleService;
    }

    @Override
    public ProductionBatchCostRDto getBatchCost(UUID batchId) {
        var batch = productionBatchRepository.findById(batchId).orElseThrow(() -> notFound(batchId));
        var estimated = batch.getStatus() != ProductionBatchStatus.COMPLETED;
        var lines = batch.getInputs().stream()
                .map(input -> lineOf(input, estimated))
                .toList();
        var materialCost = materialCostOf(lines);
        var unitCost = perUnit(materialCost, batch.getQuantity());
        var output = articleService.getArticleById(batch.getOutputArticleId());
        return new ProductionBatchCostRDto(
                batch.getId(),
                batch.getNumber(),
                output.id(),
                output.name(),
                batch.getQuantity(),
                batch.getStatus(),
                batch.getCompletedAt(),
                estimated,
                lines,
                materialCost,
                unitCost,
                (int) lines.stream().filter(line -> line.lineCost() == null).count(),
                output.price(),
                marginPercent(unitCost, output.price()));
    }

    /** Frozen cost once completed; current purchase cost of the article before. */
    private ProductionInputCostRDto lineOf(ProductionInput input, boolean estimated) {
        var article = articleService.getArticleById(input.getArticleId());
        var unitCost = estimated ? article.cost() : input.getUnitCost();
        return new ProductionInputCostRDto(
                article.id(),
                article.code(),
                article.name(),
                article.unit(),
                input.getQuantity(),
                unitCost,
                lineCost(input.getQuantity(), unitCost));
    }

    private static @Nullable BigDecimal lineCost(BigDecimal quantity, @Nullable BigDecimal unitCost) {
        if (unitCost == null) {
            return null;
        }
        return quantity.multiply(unitCost).setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    /** Sum of the line costs, so that the detail adds up; null as soon as one input has no cost. */
    private static @Nullable BigDecimal materialCostOf(List<ProductionInputCostRDto> lines) {
        var total = BigDecimal.ZERO;
        for (var line : lines) {
            var lineCost = line.lineCost();
            if (lineCost == null) {
                return null;
            }
            total = total.add(lineCost);
        }
        return total;
    }

    private static @Nullable BigDecimal perUnit(@Nullable BigDecimal materialCost, BigDecimal quantity) {
        return materialCost == null ? null : materialCost.divide(quantity, COST_SCALE, RoundingMode.HALF_UP);
    }

    private static @Nullable Integer marginPercent(@Nullable BigDecimal unitCost, @Nullable BigDecimal price) {
        if (unitCost == null || price == null || price.signum() == 0) {
            return null;
        }
        var margin = price.subtract(unitCost).multiply(HUNDRED);
        return margin.divide(price, 0, RoundingMode.HALF_UP).intValue();
    }

    private static NotFoundException notFound(UUID batchId) {
        return new NotFoundException("Lot de production introuvable : " + batchId);
    }
}
