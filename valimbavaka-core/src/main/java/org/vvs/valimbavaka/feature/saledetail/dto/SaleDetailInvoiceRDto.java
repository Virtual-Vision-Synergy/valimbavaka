package org.vvs.valimbavaka.feature.saledetail.dto;

import java.time.Instant;
import java.util.UUID;

import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** Invoice issued for a sale (FAC-01): number, issue time and status; the full document stays on the invoice API. */
public record SaleDetailInvoiceRDto(UUID id, String number, Instant issuedAt, OperationStatus status) {}
