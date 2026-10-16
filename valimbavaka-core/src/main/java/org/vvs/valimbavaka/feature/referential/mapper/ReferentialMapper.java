package org.vvs.valimbavaka.feature.referential.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.vvs.valimbavaka.feature.referential.domain.ProductRange;
import org.vvs.valimbavaka.feature.referential.domain.SalesChannel;
import org.vvs.valimbavaka.feature.referential.domain.StockLocation;
import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReferentialMapper {

    SalesChannelRDto toRDto(SalesChannel salesChannel);

    ProductRangeRDto toRDto(ProductRange productRange);

    StockLocationRDto toRDto(StockLocation stockLocation);
}
