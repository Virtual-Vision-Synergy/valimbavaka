package org.vvs.valimbavaka.feature.supplier.controller;

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

import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierRDto;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierStatusWDto;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierWDto;
import org.vvs.valimbavaka.feature.supplier.service.SupplierService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Administrator only: each action requires the matching {@code suppliers.*} permission. */
@Tag(name = "Suppliers")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/suppliers")
@Validated
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @Operation(summary = "Paged list of suppliers, filterable by type and status")
    @ApiResponse(responseCode = "200", description = "Page of suppliers")
    @HasPermission("suppliers.read")
    public PageResponse<SupplierRDto> getSuppliers(
            @Valid PageQuery query,
            @RequestParam(required = false) @Nullable SupplierType type,
            @RequestParam(required = false) @Nullable Boolean active) {
        return supplierService.getSuppliers(query, type, active);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Supplier sheet")
    @ApiResponse(responseCode = "200", description = "Supplier")
    @ApiResponse(responseCode = "404", description = "Supplier not found (NOT_FOUND)")
    @HasPermission("suppliers.read")
    public SupplierRDto getSupplierById(@PathVariable UUID id) {
        return supplierService.getSupplierById(id);
    }

    @PostMapping
    @Operation(summary = "Creates a supplier")
    @ApiResponse(responseCode = "201", description = "Supplier created")
    @ApiResponse(responseCode = "409", description = "Name already used (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("suppliers.create")
    public ResponseEntity<SupplierRDto> createSupplier(@Valid @RequestBody SupplierWDto request) {
        var created = supplierService.createSupplier(request);
        return ResponseEntity.created(URI.create("/api/v1/suppliers/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a supplier sheet")
    @ApiResponse(responseCode = "200", description = "Supplier updated")
    @ApiResponse(responseCode = "404", description = "Supplier not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Version or name conflict (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("suppliers.update")
    public SupplierRDto updateSupplier(@PathVariable UUID id, @Valid @RequestBody SupplierWDto request) {
        return supplierService.updateSupplier(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activates or deactivates a supplier")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Supplier not found (NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("suppliers.update")
    public SupplierRDto setActive(@PathVariable UUID id, @Valid @RequestBody SupplierStatusWDto request) {
        return supplierService.setActive(id, request.active());
    }
}
