package org.vvs.valimbavaka.feature.dashboard.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.dashboard.dto.DashboardRDto;
import org.vvs.valimbavaka.feature.dashboard.service.DashboardService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Daily cockpit (DSH-01): the key indicators of today, read by the administrator's dashboard. */
@Tag(name = "Dashboard")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/dashboard")
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(
            summary = "Daily cockpit",
            description = "Turnover, sales, deliveries and stock gap of today, compared with the day before.")
    @ApiResponse(responseCode = "200", description = "Indicators of today")
    @HasPermission("reports.read")
    public DashboardRDto getDashboard() {
        return dashboardService.getDashboard();
    }
}
