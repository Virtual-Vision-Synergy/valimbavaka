package org.vvs.valimbavaka.feature.shooting.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.shooting.domain.Shooting;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingItem;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingItemRDto;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingRDto;

/** Read-only: an exit is created through {@link Shooting#open}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ShootingMapper {

    /** @param late computed by the service with its clock ({@link Shooting#isLateAt}) */
    @Mapping(target = "takenById", source = "shooting.createdBy")
    @Mapping(target = "takenBy", source = "shooting.takenByName")
    @Mapping(target = "returnedBy", source = "shooting.returnedByName")
    @Mapping(target = "late", source = "late")
    ShootingRDto toRDto(Shooting shooting, boolean late);

    ShootingItemRDto toItemRDto(ShootingItem item);
}
