package org.vvs.valimbavaka.feature.invoice.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.invoice.domain.Invoice;
import org.vvs.valimbavaka.feature.invoice.domain.InvoiceLine;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceLineRDto;
import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;

/** Read-only: an invoice is created through {@link Invoice#issue}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface InvoiceMapper {

    InvoiceRDto toRDto(Invoice invoice);

    InvoiceLineRDto toLineRDto(InvoiceLine line);
}
