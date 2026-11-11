package org.vvs.valimbavaka.feature.report.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;
import org.vvs.valimbavaka.feature.report.service.ReportService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Daily, monthly and yearly reports (RPT-01), filterable by sales channel and product range, administrator only. */
@Tag(name = "Reports")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/reports")
@Validated
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(
            summary = "Report of a day, a month or a year",
            description = "The period is the day, calendar month or calendar year containing date (default: month,"
                    + " today in Indian/Antananarivo). Only validated sales count; a range filter counts the lines"
                    + " of that range.")
    @ApiResponse(responseCode = "200", description = "Effective period, filters applied and sales indicators")
    @ApiResponse(responseCode = "422", description = "Period that has not begun yet (VALIDATION_FAILED)")
    @HasPermission("reports.read")
    public ReportRDto getReport(
            @Parameter(description = "DAY, MONTH (default) or YEAR") @RequestParam(required = false) @Nullable
                    ReportGranularity granularity,
            @Parameter(description = "A business day of the period, yyyy-MM-dd (default: today)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable
                    LocalDate date,
            @Parameter(description = "Only this sales channel (default: all)") @RequestParam(required = false) @Nullable
                    UUID salesChannelId,
            @Parameter(description = "Only this product range (default: all)") @RequestParam(required = false) @Nullable
                    UUID rangeId) {
        return reportService.getReport(granularity, date, salesChannelId, rangeId);
    }
}
