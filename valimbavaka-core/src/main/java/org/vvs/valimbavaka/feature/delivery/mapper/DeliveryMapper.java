package org.vvs.valimbavaka.feature.delivery.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLine;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryDriverRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderLineRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DeliveryMapper {

    DeliveryOrderRDto toRDto(DeliveryOrder order);

    DeliveryOrderLineRDto toLineRDto(DeliveryOrderLine line);

    DeliveryDriverRDto toDriverRDto(UserRDto user);
}
