package org.vvs.valimbavaka.feature.stock.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.stock.domain.StockMovement;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementRDto;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface StockMovementMapper {
    StockMovementRDto toRDto(StockMovement movement);
}
