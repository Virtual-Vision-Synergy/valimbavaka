package org.vvs.valimbavaka.feature.delivery.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderFilter;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryDriverRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderWDto;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryOrderService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Orders to deliver (LIV-01): entered by a seller or the administrator, each one is added to its driver's round. */
@Tag(name = "Deliveries")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/deliveries")
@Validated
public class DeliveryController {

    private final DeliveryOrderService deliveryOrderService;

    public DeliveryController(DeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = deliveryOrderService;
    }

    @GetMapping
    @Operation(summary = "Paged orders to deliver, filterable by driver, day and status; a driver only sees his own")
    @ApiResponse(responseCode = "200", description = "Page of orders")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("deliveries.read")
    public PageResponse<DeliveryOrderRDto> getOrders(
            @Valid PageQuery query,
            @RequestParam(required = false) @Nullable UUID driverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate date,
            @RequestParam(required = false) @Nullable DeliveryStatus status) {
        return deliveryOrderService.getOrders(query, new DeliveryOrderFilter(driverId, date, status));
    }

    @GetMapping("/drivers")
    @Operation(summary = "Active delivery drivers that can be assigned to an order")
    @ApiResponse(responseCode = "200", description = "Page of drivers")
    @HasPermission("deliveries.create")
    public PageResponse<DeliveryDriverRDto> getDrivers(@Valid PageQuery query) {
        return deliveryOrderService.getDrivers(query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Order to deliver")
    @ApiResponse(responseCode = "200", description = "Order")
    @ApiResponse(responseCode = "404", description = "Order not found (NOT_FOUND)")
    @HasPermission("deliveries.read")
    public DeliveryOrderRDto getOrder(@PathVariable UUID id) {
        return deliveryOrderService.getOrder(id);
    }

    @PostMapping
    @Operation(summary = "Creates an order to deliver and adds it at the end of the round of its driver")
    @ApiResponse(responseCode = "201", description = "Order created with the TO_PREPARE status")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("deliveries.create")
    public ResponseEntity<DeliveryOrderRDto> createOrder(@Valid @RequestBody DeliveryOrderWDto request) {
        var created = deliveryOrderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/v1/deliveries/" + created.id()))
                .body(created);
    }
}
