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
import org.vvs.valimbavaka.feature.delivery.service.DeliveryUndoService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Time-limited undo of the last action on a delivery (LIV-05): delivered or failed, back to the previous status. */
@Tag(name = "Deliveries")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/deliveries")
@Validated
public class DeliveryUndoController {

    private final DeliveryUndoService deliveryUndoService;

    public DeliveryUndoController(DeliveryUndoService deliveryUndoService) {
        this.deliveryUndoService = deliveryUndoService;
    }

    @PostMapping("/{id}/undo")
    @Operation(summary = "Undoes the last action (delivered or failed) within 5 minutes; restores the previous status")
    @ApiResponse(responseCode = "200", description = "Order back to its previous status, sale or return cancelled")
    @ApiResponse(responseCode = "404", description = "Order not found or assigned to another driver (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "No recent action to undo, delay over or stock missing (CONFLICT)")
    @HasPermission("deliveries.undo")
    public DeliveryOrderRDto undoLastAction(@PathVariable UUID id) {
        return deliveryUndoService.undoLastAction(id);
    }
}
