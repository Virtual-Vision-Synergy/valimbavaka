package org.vvs.valimbavaka.feature.invoice.mapper;

import org.jspecify.annotations.Nullable;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.invoice.domain.SalesBookEntry;
import org.vvs.valimbavaka.feature.invoice.domain.SalesBookEntryType;
import org.vvs.valimbavaka.feature.invoice.dto.SalesBookRowRDto;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** Read-only: turns a line of the sales book into its row, with the French labels printed in the files. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface SalesBookMapper {

    SalesBookRowRDto toRDto(SalesBookEntry entry);

    default String typeLabel(SalesBookEntryType type) {
        return type == SalesBookEntryType.CREDIT_NOTE ? "Avoir" : "Facture";
    }

    default @Nullable String statusLabel(@Nullable OperationStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case VALIDATED -> "Validée";
            case CANCELLED -> "Annulée";
            case REVERSED -> "Contre-passée";
        };
    }
}
