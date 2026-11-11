package org.vvs.valimbavaka.feature.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.excel.annotation.ExcelColumn;

/**
 * One line of the sales book (FAC-06). The annotated components are the columns of the xlsx and PDF exports. A credit
 * note carries negative amounts; the status is that of the invoice and stays empty on a credit note line.
 */
public record SalesBookRowRDto(
        UUID id,

        @ExcelColumn(header = "Date", order = 1, width = 12) LocalDate businessDate,

        @ExcelColumn(header = "Type", order = 2, width = 10)
        String type,

        @ExcelColumn(header = "Numéro", order = 3, width = 16)
        String number,

        @ExcelColumn(header = "Facture d'origine", order = 4, width = 16) @Nullable
        String invoiceNumber,

        @ExcelColumn(header = "Vente", order = 5, width = 12)
        String saleNumber,

        @ExcelColumn(header = "Canal", order = 6, width = 16) @Nullable
        String salesChannelLabel,

        @ExcelColumn(header = "Vendeuse", order = 7, width = 22) @Nullable
        String sellerName,

        @ExcelColumn(header = "Règlement", order = 8, width = 14)
        String paymentMethod,

        @ExcelColumn(header = "Statut", order = 9, width = 14) @Nullable
        String status,

        @ExcelColumn(header = "Sous-total (Ar)", order = 10, format = "#,##0.00", width = 16)
        BigDecimal subtotal,

        @ExcelColumn(header = "Remise (Ar)", order = 11, format = "#,##0.00", width = 14)
        BigDecimal discountAmount,

        @ExcelColumn(header = "Total (Ar)", order = 12, format = "#,##0.00", width = 16)
        BigDecimal total,

        @ExcelColumn(header = "Motif de l'avoir", order = 13, width = 40) @Nullable
        String reason) {}
