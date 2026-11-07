package org.vvs.valimbavaka.feature.commission.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.commission.domain.CommissionRule;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleRDto;

/** Read-only: creation and update go through the business methods of {@link CommissionRule}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CommissionRuleMapper {

    CommissionRuleRDto toRDto(CommissionRule rule);
}
