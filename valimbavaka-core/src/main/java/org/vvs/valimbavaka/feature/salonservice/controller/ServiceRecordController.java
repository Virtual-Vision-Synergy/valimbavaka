package org.vvs.valimbavaka.feature.salonservice.controller;

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

import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordWDto;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Performed salon services (SRV-02): read with {@code services.read}, recorded with {@code service-records.create}. */
@Tag(name = "Service records")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/service-records")
@Validated
public class ServiceRecordController {

    private final ServiceRecordService serviceRecordService;

    public ServiceRecordController(ServiceRecordService serviceRecordService) {
        this.serviceRecordService = serviceRecordService;
    }

    @GetMapping
    @Operation(summary = "Paged list of performed salon services, most recent first")
    @ApiResponse(responseCode = "200", description = "Page of performed services")
    @HasPermission("services.read")
    public PageResponse<ServiceRecordRDto> getServiceRecords(@Valid PageQuery query) {
        return serviceRecordService.getServiceRecords(query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Performed salon service")
    @ApiResponse(responseCode = "200", description = "Performed service")
    @ApiResponse(responseCode = "404", description = "Performed service not found (NOT_FOUND)")
    @HasPermission("services.read")
    public ServiceRecordRDto getServiceRecordById(@PathVariable UUID id) {
        return serviceRecordService.getServiceRecordById(id);
    }

    @PostMapping
    @Operation(summary = "Records a performed salon service: its products leave the salon stock")
    @ApiResponse(responseCode = "201", description = "Performed service recorded")
    @ApiResponse(responseCode = "404", description = "Salon service or article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Insufficient salon stock (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or inactive service (VALIDATION_FAILED)")
    @HasPermission("service-records.create")
    public ResponseEntity<ServiceRecordRDto> recordService(@Valid @RequestBody ServiceRecordWDto request) {
        var created = serviceRecordService.recordService(request);
        return ResponseEntity.created(URI.create("/api/v1/service-records/" + created.id()))
                .body(created);
    }
}
