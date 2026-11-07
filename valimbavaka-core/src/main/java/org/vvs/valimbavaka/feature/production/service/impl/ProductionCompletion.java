package org.vvs.valimbavaka.feature.production.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;

/**
 * Completion of a batch (PRD-02): freezes the input costs, then records in the caller's transaction one outgoing
 * PRODUCTION movement per input and one incoming movement for the output, all in the warehouse. A refused movement
 * (insufficient stock: 409) rolls back the whole completion: never one movement without the other.
 */
@Component
class ProductionCompletion {

    /** Stable code of the location where production consumes and stores (hypothesis of PRD-01). */
    static final String PRODUCTION_LOCATION = "WAREHOUSE";

    private final StockMovementService stockMovementService;
    private final ArticleService articleService;
    private final ReferentialService referentialService;
    private final Clock clock;

    ProductionCompletion(
            StockMovementService stockMovementService,
            ArticleService articleService,
            ReferentialService referentialService,
            Clock clock) {
        this.stockMovementService = stockMovementService;
        this.articleService = articleService;
        this.referentialService = referentialService;
        this.clock = clock;
    }

    /** Must be called in a transaction, on a batch locked or just created. */
    void complete(ProductionBatch batch) {
        var inputIds = batch.getInputs().stream().map(ProductionInput::getArticleId).toList();
        batch.complete(articleService.getCosts(inputIds), clock.instant());
        var warehouseId = referentialService.requireActiveStockLocationId(PRODUCTION_LOCATION);
        // Inputs in a stable order, output last: two completions never take the stock locks in opposite orders.
        batch.getInputs().stream()
                .sorted(Comparator.comparing(ProductionInput::getArticleId))
                .forEach(input -> move(batch, input.getArticleId(), warehouseId, input.getQuantity().negate()));
        move(batch, batch.getOutputArticleId(), warehouseId, batch.getQuantity());
    }

    private void move(ProductionBatch batch, UUID articleId, UUID locationId, BigDecimal quantity) {
        stockMovementService.record(
                new StockMovementCommand(articleId, locationId, StockMovementType.PRODUCTION, quantity, batch.getId()));
    }
}
