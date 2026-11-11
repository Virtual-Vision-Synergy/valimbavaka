package org.vvs.valimbavaka.feature.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * Invoice line: the article name and prices as they were when the invoice was issued. The annotated components are
 * the columns of the invoice PDF (FAC-03).
 */
public record InvoiceLineRDto(
        UUID articleId,

        @ExcelColumn(header = "Désignation", order = 1, width = 40)
        String label,

        @ExcelColumn(header = "Qté", order = 2, width = 8) BigDecimal quantity,

        @ExcelColumn(header = "Prix unitaire (Ar)", order = 3, width = 16)
        BigDecimal unitPrice,

        @ExcelColumn(header = "Total (Ar)", order = 4, width = 16)
        BigDecimal lineTotal) {}
