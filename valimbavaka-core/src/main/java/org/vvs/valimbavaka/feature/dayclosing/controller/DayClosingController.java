package org.vvs.valimbavaka.feature.dayclosing.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Closing reconciliation of today (JR-04), read by the administrator's closing screen. */
@Tag(name = "Business days")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/days")
@Validated
public class DayClosingController {

    private final DayClosingService dayClosingService;

    public DayClosingController(DayClosingService dayClosingService) {
        this.dayClosingService = dayClosingService;
    }

    @GetMapping("/closing")
    @Operation(
            summary = "Closing reconciliation of today",
            description = "Per location and article: transferred, returned, sold, delivered, salon and shooting"
                    + " quantities, the theoretical quantity left and the gap with the system stock.")
    @ApiResponse(responseCode = "200", description = "Reconciliation of today")
    @ApiResponse(responseCode = "409", description = "Today's day is not opened (CONFLICT)")
    @HasPermission("days.close")
    public DayClosingRDto getClosing() {
        return dayClosingService.getClosing();
    }
}
