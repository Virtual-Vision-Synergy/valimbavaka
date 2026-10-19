package org.vvs.valimbavaka.feature.referential.controller;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Référentiels métier en lecture seule (SYS-05), réservés aux utilisateurs authentifiés. */
@RestController
@RequestMapping("/api/v1")
@Validated
public class ReferentialController {

    private final ReferentialService referentialService;

    public ReferentialController(ReferentialService referentialService) {
        this.referentialService = referentialService;
    }

    @GetMapping("/sales-channels")
    @HasPermission("referentials.read")
    public PageResponse<SalesChannelRDto> getSalesChannels(@Valid PageQuery query) {
        return referentialService.getSalesChannels(query);
    }

    @GetMapping("/product-ranges")
    @HasPermission("referentials.read")
    public PageResponse<ProductRangeRDto> getProductRanges(@Valid PageQuery query) {
        return referentialService.getProductRanges(query);
    }

    @GetMapping("/stock-locations")
    @HasPermission("referentials.read")
    public PageResponse<StockLocationRDto> getStockLocations(@Valid PageQuery query) {
        return referentialService.getStockLocations(query);
    }
}
