package org.vvs.valimbavaka.feature.stock.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.stock.dto.StockAlertRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockValuationRDto;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Read only: a stock is never written through the API, it results from the typed movements (STK-01). */
@Tag(name = "Stocks")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/stocks")
@Validated
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    @Operation(summary = "Paged stock of the active articles: quantity per location and total")
    @ApiResponse(responseCode = "200", description = "Page of stocks")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("stocks.read")
    public PageResponse<StockRDto> getStocks(@Valid PageQuery query) {
        return stockService.getStocks(query);
    }

    @GetMapping("/alerts")
    @Operation(summary = "Active articles under their minimum threshold, with shortage and suggested quantity")
    @ApiResponse(responseCode = "200", description = "Page of threshold alerts, most urgent first")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("stocks.read")
    public PageResponse<StockAlertRDto> getAlerts(@Valid PageQuery query) {
        return stockService.getAlerts(query);
    }

    @GetMapping("/valuation")
    @Operation(summary = "Quantity and value at cost of the stock per location, in Ariary")
    @ApiResponse(responseCode = "200", description = "Valuation of the stock")
    @HasPermission("stocks.value")
    public StockValuationRDto getValuation() {
        return stockService.getValuation();
    }

    @GetMapping("/{articleId}")
    @Operation(summary = "Stock of one article: quantity per location and total")
    @ApiResponse(responseCode = "200", description = "Stock of the article")
    @ApiResponse(responseCode = "404", description = "Unknown article (NOT_FOUND)")
    @HasPermission("stocks.read")
    public StockRDto getStock(@PathVariable UUID articleId) {
        return stockService.getStock(articleId);
    }
}
