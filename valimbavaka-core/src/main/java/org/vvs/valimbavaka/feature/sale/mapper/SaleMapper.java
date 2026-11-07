package org.vvs.valimbavaka.feature.sale.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleLine;
import org.vvs.valimbavaka.feature.sale.dto.SaleLineRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;

/** Read-only: a sale is created through {@link Sale#record}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface SaleMapper {

    SaleRDto toRDto(Sale sale);

    SaleLineRDto toLineRDto(SaleLine line);
}
