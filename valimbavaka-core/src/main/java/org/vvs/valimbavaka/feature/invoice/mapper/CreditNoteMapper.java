package org.vvs.valimbavaka.feature.invoice.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.invoice.domain.CreditNote;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceLine;
import org.vvs.valimbavaka.feature.invoice.dto.CreditNoteRDto;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceLineRDto;

/** Read-only: a credit note is created through {@link CreditNote#issue}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CreditNoteMapper {

    CreditNoteRDto toRDto(CreditNote creditNote);

    InvoiceLineRDto toLineRDto(InvoiceLine line);
}
