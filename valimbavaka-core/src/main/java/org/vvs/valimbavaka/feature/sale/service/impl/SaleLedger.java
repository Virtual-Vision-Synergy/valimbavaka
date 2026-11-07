package org.vvs.valimbavaka.feature.sale.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleRecordedEvent;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;

/**
 * Takes the stock of a recorded sale out of the location of its channel (one {@code SALE} movement per line,
 * referencing the sale), traces the sale in the journal and publishes {@link SaleRecordedEvent}, in the caller's
 * transaction.
 */
@Component
class SaleLedger {

    static final String RESOURCE_TYPE = "SALE";

    private final StockMovementService stockMovementService;
    private final JournalService journalService;
    private final ApplicationEventPublisher events;

    SaleLedger(
            StockMovementService stockMovementService,
            JournalService journalService,
            ApplicationEventPublisher events) {
        this.stockMovementService = stockMovementService;
        this.journalService = journalService;
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
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, sale.getId(), description(sale, channelLabel));
        events.publishEvent(new SaleRecordedEvent(sale.getId(), sale.getNumber()));
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
