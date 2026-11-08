package org.vvs.valimbavaka.feature.salonservice.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecord;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordProduct;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordProductRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;

/** Read-only: a performed service is created through {@link ServiceRecord#record}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ServiceRecordMapper {

    @Mapping(target = "performedAt", source = "createdAt")
    ServiceRecordRDto toRDto(ServiceRecord serviceRecord);

    ServiceRecordProductRDto toProductRDto(ServiceRecordProduct product);
}
