package org.vvs.valimbavaka.feature.stock.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.stock.domain.RestockOrder;
import org.vvs.valimbavaka.feature.stock.domain.RestockOrderLine;
import org.vvs.valimbavaka.feature.stock.domain.RestockOrderLineCommand;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderLineRDto;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockAlertRDto;

/** Restock order (STK-05): line content taken from a threshold alert, read view of an order and its lines. */
@Component
public class RestockOrderMapper {

    /** The quantity to order is the suggested quantity of the alert (STK-04). */
    public RestockOrderLineCommand toLineCommand(StockAlertRDto alert) {
        return new RestockOrderLineCommand(
                alert.articleId(),
                alert.code(),
                alert.name(),
                alert.unit().name(),
                alert.quantity(),
                alert.threshold(),
                alert.suggestedQuantity(),
                alert.supplierId(),
                alert.supplierName(),
                alert.leadTimeDays(),
                alert.expectedDeliveryDate());
    }

    /** @param lines lines of this order, already sorted by line number */
    public RestockOrderRDto toRDto(RestockOrder order, List<RestockOrderLine> lines) {
        return new RestockOrderRDto(
                order.getId(),
                order.getReference(),
                order.getStatus(),
                order.getCreatedAt(),
                lines.stream().map(RestockOrderMapper::toLineRDto).toList());
    }

    private static RestockOrderLineRDto toLineRDto(RestockOrderLine line) {
        return new RestockOrderLineRDto(
                line.getLineNumber(),
                line.getArticleId(),
                line.getArticleCode(),
                line.getArticleName(),
                ArticleUnit.valueOf(line.getUnit()),
                line.getStockQuantity(),
                line.getThreshold(),
                line.getQuantity(),
                line.getSupplierId(),
                line.getSupplierName(),
                line.getLeadTimeDays(),
                line.getExpectedDeliveryDate());
    }
}
