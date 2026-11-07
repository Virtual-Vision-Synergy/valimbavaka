package org.vvs.valimbavaka.feature.sale.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.dto.SaleChannelOptionRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;

/**
 * Active sales channels and the stock location each one sells from: the active location bearing the same code
 * ({@code STORE}, {@code DELIVERY}), otherwise the store ({@code FACEBOOK} and {@code PHONE} sales leave the shop).
 */
@Component
class SaleChannels {

    static final String DEFAULT_LOCATION_CODE = "STORE";

    private static final PageQuery ALL = new PageQuery(1, 100, null, "code", null);

    private final ReferentialService referentialService;

    SaleChannels(ReferentialService referentialService) {
        this.referentialService = referentialService;
    }

    /** Every active channel with its stock location, by code. */
    List<SaleChannelOptionRDto> options() {
        var locations = activeLocations();
        return referentialService.getSalesChannels(ALL).items().stream()
                .map(channel -> option(channel, locations))
                .toList();
    }

    /** 422 if the channel is unknown or inactive, or if its stock location is. */
    SaleChannelOptionRDto require(UUID salesChannelId) {
        return options().stream()
                .filter(option -> option.id().equals(salesChannelId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Le canal de vente est inconnu ou inactif."));
    }

    private Map<String, UUID> activeLocations() {
        return referentialService.getStockLocations(ALL).items().stream()
                .collect(Collectors.toMap(StockLocationRDto::code, StockLocationRDto::id));
    }

    private static SaleChannelOptionRDto option(SalesChannelRDto channel, Map<String, UUID> locations) {
        var locationId = locations.containsKey(channel.code())
                ? locations.get(channel.code())
                : locations.get(DEFAULT_LOCATION_CODE);
        if (locationId == null) {
            throw new BusinessException("L'emplacement de stock du canal %s est inconnu ou inactif."
                    .formatted(channel.label()));
        }
        return new SaleChannelOptionRDto(channel.id(), channel.code(), channel.label(), locationId);
    }
}
