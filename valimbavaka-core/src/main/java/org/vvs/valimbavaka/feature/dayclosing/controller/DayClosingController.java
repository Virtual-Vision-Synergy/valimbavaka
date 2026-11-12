package org.vvs.valimbavaka.feature.dayclosing.controller;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingSalonRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingCountsWDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingCountService;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingLockService;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingSalonService;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/**
 * Closing of today: reconciliation (JR-04), salon consumption (SRV-04), evening count with its gap reasons (JR-05)
 * and lock (JR-06).
 */
@Tag(name = "Business days")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/days")
@Validated
public class DayClosingController {

    private final DayClosingService dayClosingService;
    private final DayClosingCountService dayClosingCountService;
    private final DayClosingLockService dayClosingLockService;
    private final DayClosingSalonService dayClosingSalonService;

    public DayClosingController(
            DayClosingService dayClosingService,
            DayClosingCountService dayClosingCountService,
            DayClosingLockService dayClosingLockService,
            DayClosingSalonService dayClosingSalonService) {
        this.dayClosingService = dayClosingService;
        this.dayClosingCountService = dayClosingCountService;
        this.dayClosingLockService = dayClosingLockService;
        this.dayClosingSalonService = dayClosingSalonService;
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

    @GetMapping("/closing/salon")
    @Operation(
            summary = "Salon consumption of today against the closing",
            description = "Per article, the quantity consumed by the services performed today and the « Salon »"
                    + " column of the closing, with the two totals and whether they match.")
    @ApiResponse(responseCode = "200", description = "Salon consumption of today")
    @ApiResponse(responseCode = "409", description = "Today's day is not opened (CONFLICT)")
    @HasPermission("days.close")
    public ClosingSalonRDto getSalonConsumption() {
        return dayClosingSalonService.getSalonConsumption();
    }

    @PutMapping("/closing/counts")
    @Operation(
            summary = "Saves the evening counts of today",
            description = "Records the counted quantity of each article; a count that differs from the theoretical"
                    + " quantity needs a reason. All or nothing; a new count of a line replaces the previous one.")
    @ApiResponse(responseCode = "200", description = "Reconciliation of today with the saved counts")
    @ApiResponse(responseCode = "404", description = "Line not part of today's closing (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Today's day is not open (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or gap without reason (VALIDATION_FAILED)")
    @HasPermission("days.close")
    public DayClosingRDto saveCounts(@Valid @RequestBody DayClosingCountsWDto request) {
        return dayClosingCountService.saveCounts(request);
    }

    @PostMapping("/closing/lock")
    @Operation(
            summary = "Locks the closing of today",
            description = "Checks that every line is counted and every gap has its reason, freezes the counts, closes"
                    + " the day and records the closing with its gaps in the journal. No body.")
    @ApiResponse(responseCode = "200", description = "Reconciliation of the closed day")
    @ApiResponse(responseCode = "409", description = "Today's day is not open or already closed (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Line not counted or gap without reason (VALIDATION_FAILED)")
    @HasPermission("days.close")
    public DayClosingRDto lock() {
        return dayClosingLockService.lock();
    }
}
