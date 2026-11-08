package org.vvs.valimbavaka.feature.day.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningWDto;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Business day: the administrator opens today by dispatching the warehouse stock to the points of sale (JR-01). */
@Tag(name = "Business days")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/days")
@Validated
public class BusinessDayController {

    private final BusinessDayService dayService;

    public BusinessDayController(BusinessDayService dayService) {
        this.dayService = dayService;
    }

    @GetMapping("/opening")
    @Operation(summary = "Morning dispatch grid of today: warehouse stock, destinations, today's day if opened")
    @ApiResponse(responseCode = "200", description = "Dispatch grid")
    @HasPermission("days.open")
    public DayOpeningRDto getOpening() {
        return dayService.getOpening();
    }

    @PostMapping
    @Operation(summary = "Opens today: one validated morning transfer per destination, stock moved")
    @ApiResponse(responseCode = "201", description = "Day opened with its morning transfers")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Day already opened or not enough stock (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("days.open")
    public ResponseEntity<BusinessDayRDto> openDay(@Valid @RequestBody DayOpeningWDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dayService.openDay(request));
    }
}
