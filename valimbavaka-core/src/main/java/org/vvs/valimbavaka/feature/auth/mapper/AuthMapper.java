package org.vvs.valimbavaka.feature.auth.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.vvs.valimbavaka.feature.auth.dto.AuthRDto;
import org.vvs.valimbavaka.feature.auth.dto.CurrentUserRDto;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.TokenPair;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AuthMapper {

    CurrentUserRDto toRDto(AuthenticatedUser user);

    default AuthRDto toRDto(TokenPair tokens) {
        return new AuthRDto(tokens.accessToken(), AuthRDto.BEARER, tokens.expiresInSeconds(),
                toRDto(tokens.user()));
    }
}
