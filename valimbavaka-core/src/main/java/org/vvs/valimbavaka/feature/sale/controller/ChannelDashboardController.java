package org.vvs.valimbavaka.feature.sale.controller;

import java.time.LocalDate;

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

import org.vvs.valimbavaka.feature.sale.dto.ChannelDashboardRDto;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Sales dashboard by channel (VTE-08): turnover, number of sales and share of the total, administrator only. */
@Tag(name = "Sales")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/sales/channel-dashboard")
@Validated
public class ChannelDashboardController {

    private final ChannelDashboardService channelDashboardService;

    public ChannelDashboardController(ChannelDashboardService channelDashboardService) {
        this.channelDashboardService = channelDashboardService;
    }

    @GetMapping
    @Operation(
            summary = "Turnover, number of validated sales and share of the total turnover of each channel",
            description = "Business days from..to inclusive; to = today (Indian/Antananarivo) and from = first day of"
                    + " that month by default. Cancelled and reversed sales are not counted.")
    @ApiResponse(responseCode = "200", description = "Totals of the period and one entry per channel, best first")
    @ApiResponse(responseCode = "422", description = "from after to (VALIDATION_FAILED)")
    @HasPermission("reports.read")
    public ChannelDashboardRDto getDashboard(
            @Parameter(description = "First business day, yyyy-MM-dd")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable
                    LocalDate from,
            @Parameter(description = "Last business day, yyyy-MM-dd")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable
                    LocalDate to) {
        return channelDashboardService.getDashboard(from, to);
    }
}
