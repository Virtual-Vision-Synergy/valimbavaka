package org.vvs.valimbavaka.iam.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.vvs.valimbavaka.iam.domain.Role;
import org.vvs.valimbavaka.iam.dto.RoleRDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleMapper {
    RoleRDto toRDto(Role role);
}
