package org.vvs.valimbavaka.feature.referential.service.impl;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.mapper.ReferentialMapper;
import org.vvs.valimbavaka.feature.referential.repository.ProductRangeRepository;
import org.vvs.valimbavaka.feature.referential.repository.SalesChannelRepository;
import org.vvs.valimbavaka.feature.referential.repository.StockLocationRepository;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class ReferentialServiceImpl implements ReferentialService {

    private static final Set<String> SORT_FIELDS = Set.of("code", "label");

    private final SalesChannelRepository salesChannelRepository;
    private final ProductRangeRepository productRangeRepository;
    private final StockLocationRepository stockLocationRepository;
    private final ReferentialMapper referentialMapper;

    public ReferentialServiceImpl(SalesChannelRepository salesChannelRepository,
            ProductRangeRepository productRangeRepository, StockLocationRepository stockLocationRepository,
            ReferentialMapper referentialMapper) {
        this.salesChannelRepository = salesChannelRepository;
        this.productRangeRepository = productRangeRepository;
        this.stockLocationRepository = stockLocationRepository;
        this.referentialMapper = referentialMapper;
    }

    @Override
    public PageResponse<SalesChannelRDto> getSalesChannels(PageQuery query) {
        return PageResponse.from(salesChannelRepository.findByActiveTrue(pageable(query))
                .map(referentialMapper::toRDto));
    }

    @Override
    public PageResponse<ProductRangeRDto> getProductRanges(PageQuery query) {
        return PageResponse.from(productRangeRepository.findByActiveTrue(pageable(query))
                .map(referentialMapper::toRDto));
    }

    @Override
    public PageResponse<StockLocationRDto> getStockLocations(PageQuery query) {
        return PageResponse.from(stockLocationRepository.findByActiveTrue(pageable(query))
                .map(referentialMapper::toRDto));
    }

    @Override
    public boolean isActiveSalesChannel(UUID salesChannelId) {
        return salesChannelRepository.existsByIdAndActiveTrue(salesChannelId);
    }

    private static Pageable pageable(PageQuery query) {
        return query.toPageable(SORT_FIELDS, "label", SortDirection.ASC);
    }
}
