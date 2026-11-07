package org.vvs.valimbavaka.feature.production.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionInputRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionOperatorRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;

/** Read-only: a batch is created through {@link ProductionBatch#plan}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ProductionMapper {

    ProductionBatchRDto toRDto(ProductionBatch batch);

    ProductionInputRDto toInputRDto(ProductionInput input);

    ProductionOperatorRDto toOperatorRDto(UserRDto user);
}
