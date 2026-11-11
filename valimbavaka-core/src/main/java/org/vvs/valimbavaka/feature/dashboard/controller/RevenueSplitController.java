package org.vvs.valimbavaka.feature.dashboard.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenueSplitRDto;
import org.vvs.valimbavaka.feature.dashboard.service.RevenueSplitService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Split of today's turnover by product range and by sales channel (DSH-04), read by the administrator's cockpit. */
@Tag(name = "Dashboard")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/dashboard")
@Validated
public class RevenueSplitController {

    private final RevenueSplitService revenueSplitService;

    public RevenueSplitController(RevenueSplitService revenueSplitService) {
        this.revenueSplitService = revenueSplitService;
    }

    @GetMapping("/revenue-split")
    @Operation(
            summary = "Turnover of the day by range and channel",
            description = "Value and share in percent (one decimal, 100.0 in total, 0.0 without turnover) of each "
                    + "active range and channel for today (Madagascar time), best turnover first.")
    @ApiResponse(responseCode = "200", description = "Split of today")
    @HasPermission("reports.read")
    public RevenueSplitRDto getRevenueSplit() {
        return revenueSplitService.getToday();
    }
}
