package org.vvs.valimbavaka.feature.commission.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.domain.CommissionSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Automatic commissions (COM-02). The rate is the most precise one, rates never add up (PO decision of 07/11/2026):
 * salon service, otherwise the channel rule, otherwise the seller's own rate ({@link CommissionRateResolver}).
 */
public interface CommissionService {

    /**
     * Computes and stores the commission of an operation, in the caller's transaction, and traces it in the journal.
     * Idempotent: an operation already commissioned returns its commission unchanged. Empty when the account credited
     * is not an active seller (an administrator recording a salon service earns nothing).
     */
    Optional<CommissionRDto> calculate(CommissionSource source);

    /**
     * Commissions of the business days {@code [from, to]} (both optional, inclusive), of one seller or of all; 422 if
     * {@code from} is after {@code to}.
     */
    PageResponse<CommissionRDto> getCommissions(
            PageQuery query, @Nullable UUID sellerId, @Nullable LocalDate from, @Nullable LocalDate to);

    /** Commission of a sale or of a salon service (VTE-09), empty if none was computed. */
    Optional<CommissionRDto> findBySource(CommissionSourceType sourceType, UUID sourceId);
}
