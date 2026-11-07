package org.vvs.valimbavaka.feature.stock.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.stock.dto.RestockOrderRDto;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderWDto;
import org.vvs.valimbavaka.feature.stock.service.RestockOrderService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Restock orders (STK-05): administrator only, permission {@code stocks.restock}. No update, no delete. */
@Tag(name = "Restock orders")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/stocks/restock-orders")
@Validated
public class RestockOrderController {

    private final RestockOrderService restockOrderService;

    public RestockOrderController(RestockOrderService restockOrderService) {
        this.restockOrderService = restockOrderService;
    }

    @PostMapping
    @Operation(summary = "Generates a restock order from the articles under their threshold")
    @ApiResponse(responseCode = "201", description = "Restock order generated")
    @ApiResponse(responseCode = "409", description = "No requested article under its threshold (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("stocks.restock")
    public ResponseEntity<RestockOrderRDto> generate(@Valid @RequestBody RestockOrderWDto request) {
        var created = restockOrderService.generate(request);
        return ResponseEntity.created(URI.create("/api/v1/stocks/restock-orders/" + created.id()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Paged list of the restock orders with their lines, newest first")
    @ApiResponse(responseCode = "200", description = "Page of restock orders")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("stocks.restock")
    public PageResponse<RestockOrderRDto> getRestockOrders(@Valid PageQuery query) {
        return restockOrderService.getRestockOrders(query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Restock order with its lines")
    @ApiResponse(responseCode = "200", description = "Restock order")
    @ApiResponse(responseCode = "404", description = "Restock order not found (NOT_FOUND)")
    @HasPermission("stocks.restock")
    public RestockOrderRDto getRestockOrderById(@PathVariable UUID id) {
        return restockOrderService.getRestockOrderById(id);
    }
}
