package org.vvs.valimbavaka.feature.delivery.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.delivery.dto.DeliveryFailureWDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryFailureService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Failure of a delivery (LIV-04): the driver gives a reason and the articles come back to stock. */
@Tag(name = "Deliveries")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request or unknown reason (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/deliveries")
@Validated
public class DeliveryFailureController {

    private final DeliveryFailureService deliveryFailureService;

    public DeliveryFailureController(DeliveryFailureService deliveryFailureService) {
        this.deliveryFailureService = deliveryFailureService;
    }

    @PostMapping("/{id}/failure")
    @Operation(summary = "Reports a failed delivery with its reason; the articles go back from delivery to warehouse")
    @ApiResponse(responseCode = "200", description = "Order FAILED, stock return recorded")
    @ApiResponse(responseCode = "404", description = "Order not found or assigned to another driver (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Order already closed or articles not in delivery (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Missing reason or comment too long (VALIDATION_FAILED)")
    @HasPermission("deliveries.fail")
    public DeliveryOrderRDto reportFailure(@PathVariable UUID id, @Valid @RequestBody DeliveryFailureWDto request) {
        return deliveryFailureService.reportFailure(id, request);
    }
}
