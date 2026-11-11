package org.vvs.valimbavaka.feature.sale.service;

import org.vvs.valimbavaka.feature.sale.domain.SaleFilter;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Reading of the recorded sales (VTE-07); recording stays in {@link SaleService}. */
public interface SaleQueryService {

    /**
     * Sales of one business day (today in Madagascar, Indian/Antananarivo, when the filter has no day), most recent
     * first, every status included (validated, cancelled, reversed). A connected active seller only reads the sales
     * credited to her: her own id replaces the seller filter. Sort on {@code createdAt}, {@code number} or
     * {@code total}; any other field falls back to {@code createdAt}. The search term is not applied.
     */
    PageResponse<SaleSummaryRDto> getSales(PageQuery query, SaleFilter filter);
}
