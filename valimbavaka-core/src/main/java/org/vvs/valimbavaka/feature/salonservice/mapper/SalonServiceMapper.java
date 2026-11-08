package org.vvs.valimbavaka.feature.salonservice.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.salonservice.domain.ConsumedProduct;
import org.vvs.valimbavaka.feature.salonservice.domain.SalonService;
import org.vvs.valimbavaka.feature.salonservice.dto.ConsumedProductRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceRDto;

/** Read-only: creation and update go through the business methods of {@link SalonService}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface SalonServiceMapper {

    SalonServiceRDto toRDto(SalonService service);

    ConsumedProductRDto toRDto(ConsumedProduct product);
}
