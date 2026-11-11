package org.vvs.valimbavaka.feature.dayoverview.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.dayoverview.dto.DayLiveRDto;
import org.vvs.valimbavaka.feature.dayoverview.service.DayLiveService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Live view of today (JR-03): sales, deliveries and salon services, read by the administrator's screen. */
@Tag(name = "Business days")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/days")
@Validated
public class DayLiveController {

    private final DayLiveService dayLiveService;

    public DayLiveController(DayLiveService dayLiveService) {
        this.dayLiveService = dayLiveService;
    }

    @GetMapping("/live")
    @Operation(
            summary = "Live view of today",
            description = "Indicators of sales, deliveries and salon services of today, and the latest operations.")
    @ApiResponse(responseCode = "200", description = "Snapshot of today")
    @HasPermission("reports.read")
    public DayLiveRDto getLive() {
        return dayLiveService.getLive();
    }
}
