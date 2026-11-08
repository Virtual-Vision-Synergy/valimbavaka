package org.vvs.valimbavaka.feature.salonservice.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceStatusWDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceWDto;
import org.vvs.valimbavaka.feature.salonservice.service.SalonCatalogService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Salon services catalogue (SRV-01): read with {@code services.read}, written with {@code services.create/update}. */
@Tag(name = "Salon services")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/salon-services")
@Validated
public class SalonServiceController {

    private final SalonCatalogService catalogService;

    public SalonServiceController(SalonCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Paged list of salon services, filterable by status")
    @ApiResponse(responseCode = "200", description = "Page of salon services")
    @HasPermission("services.read")
    public PageResponse<SalonServiceRDto> getServices(
            @Valid PageQuery query, @RequestParam(required = false) @Nullable Boolean active) {
        return catalogService.getServices(query, active);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Salon service")
    @ApiResponse(responseCode = "200", description = "Salon service")
    @ApiResponse(responseCode = "404", description = "Salon service not found (NOT_FOUND)")
    @HasPermission("services.read")
    public SalonServiceRDto getServiceById(@PathVariable UUID id) {
        return catalogService.getServiceById(id);
    }

    @PostMapping
    @Operation(summary = "Creates an active salon service")
    @ApiResponse(responseCode = "201", description = "Salon service created")
    @ApiResponse(responseCode = "409", description = "Name already used (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or unknown article (VALIDATION_FAILED)")
    @HasPermission("services.create")
    public ResponseEntity<SalonServiceRDto> createService(@Valid @RequestBody SalonServiceWDto request) {
        var created = catalogService.createService(request);
        return ResponseEntity.created(URI.create("/api/v1/salon-services/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a salon service")
    @ApiResponse(responseCode = "200", description = "Salon service updated")
    @ApiResponse(responseCode = "404", description = "Salon service not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Version or name conflict (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or unknown article (VALIDATION_FAILED)")
    @HasPermission("services.update")
    public SalonServiceRDto updateService(@PathVariable UUID id, @Valid @RequestBody SalonServiceWDto request) {
        return catalogService.updateService(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activates or deactivates a salon service")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Salon service not found (NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("services.update")
    public SalonServiceRDto setActive(@PathVariable UUID id, @Valid @RequestBody SalonServiceStatusWDto request) {
        return catalogService.setActive(id, request.active());
    }
}
