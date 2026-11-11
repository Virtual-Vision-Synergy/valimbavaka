package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Outcome of a validation (COM-03): how many commissions were validated, and for which total, in ariary. */
public record CommissionValidationRDto(
        UUID sellerId,
        String sellerName,
        LocalDate from,
        LocalDate to,
        long validatedCount,
        BigDecimal validatedAmount) {}
