package org.vvs.valimbavaka.feature.invoice.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** A line of an invoice before it is issued: the article name and the prices as they were on the sale. */
public record InvoiceLineDraft(
        UUID articleId, String label, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
