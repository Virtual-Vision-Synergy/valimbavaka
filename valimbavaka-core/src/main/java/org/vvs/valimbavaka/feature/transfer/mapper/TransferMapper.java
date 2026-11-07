package org.vvs.valimbavaka.feature.transfer.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLine;
import org.vvs.valimbavaka.feature.transfer.dto.TransferLineRDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface TransferMapper {

    TransferRDto toRDto(Transfer transfer);

    TransferLineRDto toLineRDto(TransferLine line);
}
