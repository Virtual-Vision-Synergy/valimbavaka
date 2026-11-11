package org.vvs.valimbavaka.feature.commission.service;

import java.time.YearMonth;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.dto.CommissionFollowUpRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Follow-up of the commissions and of the monthly target (COM-04). A seller only reads her own figures: the server
 * imposes her id and ignores {@code sellerId}. The administrator reads any seller's and must name her.
 */
public interface CommissionFollowUpService {

    /**
     * Totals of the month (default: the current month in Madagascar) of a seller, cancelled commissions left out, with
     * the progress towards her monthly target. 422 when the administrator names no seller, 404 when the id is not a
     * seller's.
     */
    CommissionFollowUpRDto getFollowUp(@Nullable UUID sellerId, @Nullable YearMonth month);

    /**
     * Commission of every sale and salon service of the month of a seller (base, rate, amount, status), most recent
     * first by default. Cancelled commissions are listed with their status but never counted in the totals.
     */
    PageResponse<CommissionRDto> getLines(PageQuery query, @Nullable UUID sellerId, @Nullable YearMonth month);
}
