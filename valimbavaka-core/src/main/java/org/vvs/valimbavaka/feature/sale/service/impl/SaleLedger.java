package org.vvs.valimbavaka.feature.sale.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleCancelledEvent;
import org.vvs.valimbavaka.feature.sale.domain.SaleRecordedEvent;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;

/**
 * Takes the stock of a recorded sale out of the location of its channel (one {@code SALE} movement per line,
 * referencing the sale), counts one use of the promotion that gave its discount (PRO-03), traces the sale in the
 * journal and publishes {@link SaleRecordedEvent}, in the caller's transaction.
 */
@Component
class SaleLedger {

    static final String RESOURCE_TYPE = "SALE";

    private final StockMovementService stockMovementService;
    private final JournalService journalService;
    private final PromotionService promotionService;
    private final ApplicationEventPublisher events;

    SaleLedger(
            StockMovementService stockMovementService,
            JournalService journalService,
            PromotionService promotionService,
            ApplicationEventPublisher events) {
        this.stockMovementService = stockMovementService;
        this.journalService = journalService;
        this.promotionService = promotionService;
        this.events = events;
    }

    void post(Sale sale, String channelLabel) {
        var movements = sale.getLines().stream()
                .map(line -> new StockMovementCommand(
                        line.getArticleId(),
                        sale.getStockLocationId(),
                        StockMovementType.SALE,
                        line.getQuantity().negate(),
                        sale.getId()))
                .toList();
        stockMovementService.recordAll(movements);
        var promotionId = sale.getPromotionId();
        if (promotionId != null) {
            promotionService.recordUsage(promotionId);
        }
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, sale.getId(), description(sale, channelLabel));
        events.publishEvent(new SaleRecordedEvent(sale.getId(), sale.getNumber()));
    }

    /**
     * Undoes {@link #post}: the articles come back into the location they left, as {@code RETURN} movements
     * referencing the sale (a {@code SALE} movement only takes stock out); the use of the promotion is given back
     * (PRO-03); the cancellation is journaled and published.
     */
    void unpost(Sale sale, String reason) {
        var movements = sale.getLines().stream()
                .map(line -> new StockMovementCommand(
                        line.getArticleId(),
                        sale.getStockLocationId(),
                        StockMovementType.RETURN,
                        line.getQuantity(),
                        sale.getId()))
                .toList();
        stockMovementService.recordAll(movements);
        var promotionId = sale.getPromotionId();
        if (promotionId != null) {
            promotionService.releaseUsage(promotionId);
        }
        journalService.record(
                JournalAction.CANCEL,
                RESOURCE_TYPE,
                sale.getId(),
                "Annulation de la vente %s : %s".formatted(sale.getNumber(), reason));
        events.publishEvent(new SaleCancelledEvent(sale.getId(), sale.getNumber()));
    }

    /** E.g. {@code Vente VTE-0042 · Boutique · 3 article(s) · 64000.00 Ar · Espèces}. */
    private static String description(Sale sale, String channelLabel) {
        return "Vente %s · %s · %d article(s) · %s Ar · %s"
                .formatted(
                        sale.getNumber(),
                        channelLabel,
                        sale.getLines().size(),
                        sale.getTotal().toPlainString(),
                        sale.getPaymentMethod().label());
    }
}
