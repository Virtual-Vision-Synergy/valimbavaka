package org.vvs.valimbavaka.feature.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Invoice line: the article name and prices as they were when the invoice was issued. */
public record InvoiceLineRDto(
        UUID articleId, String label, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
