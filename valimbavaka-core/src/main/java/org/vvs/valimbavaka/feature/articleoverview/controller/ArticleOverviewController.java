package org.vvs.valimbavaka.feature.articleoverview.controller;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.articleoverview.dto.ArticleOverviewRDto;
import org.vvs.valimbavaka.feature.articleoverview.service.ArticleOverviewService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Article sheet at 360° (ART-06): read only, one call for the whole screen. */
@Tag(name = "Articles")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/articles")
@Validated
public class ArticleOverviewController {

    private final ArticleOverviewService articleOverviewService;

    public ArticleOverviewController(ArticleOverviewService articleOverviewService) {
        this.articleOverviewService = articleOverviewService;
    }

    @GetMapping("/{articleId}/overview")
    @Operation(
            summary = "Article sheet at 360°",
            description = "Identity, stock per location, active promotions, last 10 batches and last 10 movements.")
    @ApiResponse(responseCode = "200", description = "Sheet of the article")
    @ApiResponse(responseCode = "404", description = "Unknown article (NOT_FOUND)")
    @HasPermission("articles.read")
    public ArticleOverviewRDto getOverview(@PathVariable UUID articleId) {
        return articleOverviewService.getOverview(articleId);
    }
}
