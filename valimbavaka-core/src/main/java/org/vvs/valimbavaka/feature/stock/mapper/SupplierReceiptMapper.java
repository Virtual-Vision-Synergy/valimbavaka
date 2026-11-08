package org.vvs.valimbavaka.feature.stock.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.stock.domain.SupplierReceipt;
import org.vvs.valimbavaka.feature.stock.domain.SupplierReceiptLine;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptLineRDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptRDto;

/** Read view of a supplier receipt (STK-06); costs rounded to the ariary cent (2 decimals, half up). */
@Component
public class SupplierReceiptMapper {

    private static final int COST_SCALE = 2;

    public SupplierReceiptRDto toRDto(SupplierReceipt receipt) {
        var lines = receipt.getLines().stream()
                .map(SupplierReceiptMapper::toLineRDto)
                .toList();
        var total = lines.stream().map(SupplierReceiptLineRDto::lineCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SupplierReceiptRDto(
                receipt.getId(),
                receipt.getNumber(),
                receipt.getStatus(),
                receipt.getSupplierId(),
                receipt.getSupplierName(),
                receipt.getLocationId(),
                receipt.getRestockOrderId(),
                receipt.getNote(),
                receipt.getCreatedAt(),
                total.setScale(COST_SCALE, RoundingMode.HALF_UP),
                lines);
    }

    private static SupplierReceiptLineRDto toLineRDto(SupplierReceiptLine line) {
        var lineCost = line.getQuantity().multiply(line.getUnitCost()).setScale(COST_SCALE, RoundingMode.HALF_UP);
        return new SupplierReceiptLineRDto(
                line.getArticleId(),
                line.getArticleCode(),
                line.getArticleName(),
                line.getQuantity(),
                line.getUnitCost(),
                lineCost);
    }
}
