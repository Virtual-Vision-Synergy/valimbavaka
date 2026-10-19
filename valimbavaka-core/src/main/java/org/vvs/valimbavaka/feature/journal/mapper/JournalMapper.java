package org.vvs.valimbavaka.feature.journal.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.journal.domain.JournalEntry;
import org.vvs.valimbavaka.feature.journal.dto.JournalEntryRDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface JournalMapper {

    JournalEntryRDto toRDto(JournalEntry entry);
}
