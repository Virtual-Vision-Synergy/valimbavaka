package org.vvs.valimbavaka.feature.sale.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.dto.SaleFormRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleWDto;

/**
 * Sales (VTE-01). {@link #record} is the port of the other features (an order delivered becomes a sale of the
 * delivery channel, LIV-03); the stock leaves through {@code StockMovementService}, never directly.
 */
public interface SaleService {

    /**
     * Records a validated sale in today's opened business day: prices frozen from the article sheets (or from the
     * command), stock taken out of the location of the channel ({@code SALE} movements), journal line and
     * {@code SaleRecordedEvent}, all or nothing. 409 if today is not opened or the location lacks stock; 422 if the
     * channel or the seller is unknown or inactive, if there is no line or more than 100, if an article appears
     * twice, is not an active finished product with a price, or if a quantity is not positive; 404 if an article is
     * unknown. Must be called in a transaction.
     */
    SaleRDto record(SaleCommand command);

    /** Sale entered at the counter: {@link #record} with the current prices of the articles. */
    SaleRDto createSale(SaleWDto request);

    /** 404 if the sale is unknown. */
    SaleRDto getSaleById(UUID saleId);

    /** Channels, active sellers and sellable articles with their stock, for the sale form. */
    SaleFormRDto getForm();
}
