package org.vvs.valimbavaka.feature.day.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.feature.day.domain.DayTransfer;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.dto.DayTransferRDto;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface BusinessDayMapper {

    BusinessDayRDto toRDto(BusinessDay day);

    DayTransferRDto toTransferRDto(DayTransfer transfer);
}
