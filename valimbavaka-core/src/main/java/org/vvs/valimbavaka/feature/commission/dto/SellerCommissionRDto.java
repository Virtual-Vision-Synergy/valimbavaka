package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.feature.commission.domain.SellerCommissionStatus;

/**
 * Commissions of one seller over a period (COM-03). Cancelled commissions are left out of every figure. The status is
 * {@code TO_VALIDATE} while at least one commission is still {@code CALCULATED}, {@code VALIDATED} otherwise.
 */
public record SellerCommissionRDto(
        UUID sellerId,
        String sellerName,
        SellerCommissionStatus status,
        long toValidateCount,
        BigDecimal toValidateAmount,
        long validatedCount,
        BigDecimal validatedAmount) {}
