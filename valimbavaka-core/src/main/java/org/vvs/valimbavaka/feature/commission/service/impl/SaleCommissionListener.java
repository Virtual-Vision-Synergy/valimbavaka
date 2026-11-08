package org.vvs.valimbavaka.feature.commission.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.commission.domain.CommissionSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.feature.sale.domain.SaleRecordedEvent;
import org.vvs.valimbavaka.feature.sale.service.SaleService;

/**
 * Computes the commission of every recorded sale (COM-02). Synchronous listener: it runs in the transaction of the
 * sale, so a sale is never recorded without its commission. A sale credited to nobody (order entered by the
 * administrator, LIV-03) earns no commission (PO decision of 07/11/2026).
 */
@Component
public class SaleCommissionListener {

    private static final Logger log = LoggerFactory.getLogger(SaleCommissionListener.class);

    private final SaleService saleService;
    private final CommissionService commissionService;

    public SaleCommissionListener(SaleService saleService, CommissionService commissionService) {
        this.saleService = saleService;
        this.commissionService = commissionService;
    }

    @EventListener
    public void onSaleRecorded(SaleRecordedEvent event) {
        var sale = saleService.getSaleById(event.saleId());
        var sellerId = sale.sellerId();
        if (sellerId == null) {
            log.info("Vente {} sans vendeuse : aucune commission", sale.number());
            return;
        }
        commissionService.calculate(new CommissionSource(
                CommissionSourceType.SALE,
                sale.id(),
                sale.number(),
                sellerId,
                sale.salesChannelId(),
                null,
                sale.businessDate(),
                sale.total(),
                saleService.getPurchaseCost(sale.id())));
    }
}
