package org.vvs.valimbavaka.iam.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.vvs.valimbavaka.iam.domain.User;
import org.vvs.valimbavaka.iam.dto.CreateUserWDto;
import org.vvs.valimbavaka.iam.dto.UpdateUserWDto;
import org.vvs.valimbavaka.iam.dto.UserRDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING,
        uses = RoleMapper.class)
public interface UserMapper {
    UserRDto toRDto(User user);

    /** Le mot de passe est encodé et le rôle résolu par le service. */
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(CreateUserWDto createUserWDto);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    void update(UpdateUserWDto updateUserWDto, @MappingTarget User user);
}
