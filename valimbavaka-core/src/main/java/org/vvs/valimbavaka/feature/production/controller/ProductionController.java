package org.vvs.valimbavaka.feature.production.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
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

import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchRDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchWDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionOperatorRDto;
import org.vvs.valimbavaka.feature.production.service.ProductionBatchService;
import org.vvs.valimbavaka.feature.production.service.ProductionOperatorService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Production batches: administrator and seller plan them (PRD-01), start them (PRD-03) and complete them, which moves
 * the stock (PRD-02). Never deleted (JNL-02).
 */
@Tag(name = "Production")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/production")
@Validated
public class ProductionController {

    private final ProductionBatchService productionBatchService;
    private final ProductionOperatorService productionOperatorService;

    public ProductionController(
            ProductionBatchService productionBatchService, ProductionOperatorService productionOperatorService) {
        this.productionBatchService = productionBatchService;
        this.productionOperatorService = productionOperatorService;
    }

    @GetMapping("/batches")
    @Operation(summary = "Paged list of production batches, most recent first, filterable by status")
    @ApiResponse(responseCode = "200", description = "Page of batches")
    @HasPermission("production.read")
    public PageResponse<ProductionBatchRDto> getBatches(
            @Valid PageQuery query, @RequestParam(required = false) @Nullable ProductionBatchStatus status) {
        return productionBatchService.getBatches(query, status);
    }

    @GetMapping("/batches/{id}")
    @Operation(summary = "Production batch with its inputs")
    @ApiResponse(responseCode = "200", description = "Batch")
    @ApiResponse(responseCode = "404", description = "Batch not found (NOT_FOUND)")
    @HasPermission("production.read")
    public ProductionBatchRDto getBatchById(@PathVariable UUID id) {
        return productionBatchService.getBatchById(id);
    }

    @PostMapping("/batches")
    @Operation(summary = "Plans a production batch: output product, quantity, responsible seller and inputs")
    @ApiResponse(responseCode = "201", description = "Batch created with status PLANNED")
    @ApiResponse(responseCode = "404", description = "Unknown article (NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("production.create")
    public ResponseEntity<ProductionBatchRDto> createBatch(@Valid @RequestBody ProductionBatchWDto request) {
        var created = productionBatchService.createBatch(request);
        return ResponseEntity.created(URI.create("/api/v1/production/batches/" + created.id()))
                .body(created);
    }

    @PostMapping("/batches/{id}/start")
    @Operation(summary = "Starts a planned batch: status IN_PROGRESS, the stock does not move")
    @ApiResponse(responseCode = "200", description = "Batch IN_PROGRESS")
    @ApiResponse(responseCode = "404", description = "Batch not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Batch already in progress or completed (CONFLICT)")
    @HasPermission("production.update")
    public ProductionBatchRDto startBatch(@PathVariable UUID id) {
        return productionBatchService.startBatch(id);
    }

    @PostMapping("/batches/{id}/completion")
    @Operation(summary = "Completes a batch: inputs out of the warehouse and output in, in one transaction")
    @ApiResponse(responseCode = "200", description = "Batch COMPLETED, stock moved, input costs frozen")
    @ApiResponse(responseCode = "404", description = "Batch not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Already completed or insufficient stock (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Warehouse unknown or inactive (VALIDATION_FAILED)")
    @HasPermission("production.complete")
    public ProductionBatchRDto completeBatch(@PathVariable UUID id) {
        return productionBatchService.completeBatch(id);
    }

    @PostMapping("/completed-batches")
    @Operation(summary = "Plans and completes a batch in one transaction (screen button \"Valider le lot\")")
    @ApiResponse(responseCode = "201", description = "Batch created with status COMPLETED, stock moved")
    @ApiResponse(responseCode = "404", description = "Unknown article (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Insufficient stock (CONFLICT): nothing is created")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("production.complete")
    public ResponseEntity<ProductionBatchRDto> createCompletedBatch(@Valid @RequestBody ProductionBatchWDto request) {
        var created = productionBatchService.createCompletedBatch(request);
        return ResponseEntity.created(URI.create("/api/v1/production/batches/" + created.id()))
                .body(created);
    }

    @GetMapping("/operators")
    @Operation(summary = "Active sellers that can be responsible for a batch")
    @ApiResponse(responseCode = "200", description = "Page of sellers")
    @HasPermission("production.read")
    public PageResponse<ProductionOperatorRDto> getOperators(@Valid PageQuery query) {
        return productionOperatorService.getOperators(query);
    }
}
