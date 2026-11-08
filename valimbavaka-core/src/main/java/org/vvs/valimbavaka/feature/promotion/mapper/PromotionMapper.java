package org.vvs.valimbavaka.feature.promotion.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionStatus;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;

/** Read-only: creation goes through {@link Promotion#create}; the status is computed by the service. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PromotionMapper {

    @Mapping(target = "status", source = "status")
    PromotionRDto toRDto(Promotion promotion, PromotionStatus status);
}
