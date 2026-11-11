package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Follow-up of one seller over a month (COM-04). Cancelled commissions are left out of every figure. The progress
 * towards the monthly target compares the revenue of the month's commissioned operations (sales and salon services)
 * with the target of the seller's user record ({@code monthlyTarget}, set by the administrator only); both are
 * {@code null} while no target is set. The progress may exceed 100 %.
 */
public record CommissionFollowUpRDto(
        UUID sellerId,
        String sellerName,
        String month,
        LocalDate from,
        LocalDate to,
        long commissionCount,
        BigDecimal revenue,
        BigDecimal commissionAmount,
        BigDecimal toValidateAmount,
        BigDecimal validatedAmount,
        @Nullable BigDecimal monthlyTarget,
        @Nullable BigDecimal targetProgressPercent) {}
