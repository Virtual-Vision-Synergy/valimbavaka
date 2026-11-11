package org.vvs.valimbavaka.feature.commission.service;

import java.time.LocalDate;

import org.vvs.valimbavaka.feature.commission.dto.CommissionValidationRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionValidationWDto;
import org.vvs.valimbavaka.feature.commission.dto.SellerCommissionRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Validation of the commissions by the administrator before payout (COM-03). Cancelled commissions are never counted
 * nor validated.
 */
public interface CommissionValidationService {

    /**
     * Sellers with commissions over the business days {@code [from, to]} (inclusive), by name, each with its status and
     * the count and total still to validate and already validated; 422 if {@code from} is after {@code to}.
     */
    PageResponse<SellerCommissionRDto> getSellerSummaries(PageQuery query, LocalDate from, LocalDate to);

    /**
     * Validates every {@code CALCULATED} commission of the seller over the period, in one transaction, and traces each
     * one in the journal. 404 if the seller has no commission over the period, 409 if all of them are already
     * validated, 422 if {@code from} is after {@code to}. A validated commission is final.
     */
    CommissionValidationRDto validate(CommissionValidationWDto request);
}
