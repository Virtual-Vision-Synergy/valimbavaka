package org.vvs.valimbavaka.feature.delivery.controller;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryCompletionService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Delivery marked as delivered in one gesture (LIV-03): the order becomes a sale of the delivery channel. */
@Tag(name = "Deliveries")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/deliveries")
@Validated
public class DeliveryCompletionController {

    private final DeliveryCompletionService deliveryCompletionService;

    public DeliveryCompletionController(DeliveryCompletionService deliveryCompletionService) {
        this.deliveryCompletionService = deliveryCompletionService;
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Marks a delivery as delivered without any input; the order becomes a delivery-channel sale")
    @ApiResponse(responseCode = "200", description = "Order DELIVERED with its sale")
    @ApiResponse(responseCode = "404", description = "Order not found or assigned to another driver (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Order closed, day not opened or stock missing (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Delivery channel unknown or inactive (VALIDATION_FAILED)")
    @HasPermission("deliveries.deliver")
    public DeliveryOrderRDto markDelivered(@PathVariable UUID id) {
        return deliveryCompletionService.markDelivered(id);
    }
}
