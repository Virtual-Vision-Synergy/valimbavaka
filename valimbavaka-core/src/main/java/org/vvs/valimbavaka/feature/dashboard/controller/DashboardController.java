package org.vvs.valimbavaka.feature.dashboard.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenueTrendRDto;
import org.vvs.valimbavaka.feature.dashboard.service.DashboardService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Figures of the administrator's dashboard (DSH-02). */
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

    @GetMapping("/revenue-trend")
    @Operation(summary = "Revenue of the last 7 days and of the last 4 weeks (validated sales and salon services)")
    @ApiResponse(responseCode = "200", description = "Revenue trend, days and weeks without revenue at 0")
    @HasPermission("reports.read")
    public RevenueTrendRDto getRevenueTrend() {
        return dashboardService.getRevenueTrend();
    }
}
