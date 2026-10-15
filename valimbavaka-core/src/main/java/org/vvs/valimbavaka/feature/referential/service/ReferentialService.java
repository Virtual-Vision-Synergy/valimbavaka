package org.vvs.valimbavaka.feature.referential.service;

import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Lecture des référentiels actifs ; interface publique pour les autres features (vente, stock). */
public interface ReferentialService {

    PageResponse<SalesChannelRDto> getSalesChannels(PageQuery query);

    PageResponse<ProductRangeRDto> getProductRanges(PageQuery query);

    PageResponse<StockLocationRDto> getStockLocations(PageQuery query);
}
