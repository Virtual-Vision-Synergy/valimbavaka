package org.vvs.valimbavaka.feature.article.controller;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ImportConflictMode;
import org.vvs.valimbavaka.feature.article.domain.ProductionUsage;
import org.vvs.valimbavaka.feature.article.dto.ArticleExportRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleImportRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleStatusWDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.feature.article.service.ArticleImportService;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;

/**
 * Reading needs {@code articles.read}; writing is administrator only ({@code articles.create} / {@code .update} /
 * {@code .import}), as is the export ({@code articles.export}).
 */
@Tag(name = "Articles")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/articles")
@Validated
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleImportService articleImportService;

    public ArticleController(ArticleService articleService, ArticleImportService articleImportService) {
        this.articleService = articleService;
        this.articleImportService = articleImportService;
    }

    @GetMapping
    @Operation(summary = "Paged list of articles, filterable by type, range, supplier and status")
    @ApiResponse(responseCode = "200", description = "Page of articles")
    @HasPermission("articles.read")
    public PageResponse<ArticleRDto> getArticles(
            @Valid PageQuery query,
            @RequestParam(required = false) @Nullable ArticleType type,
            @RequestParam(required = false) @Nullable UUID rangeId,
            @RequestParam(required = false) @Nullable UUID supplierId,
            @RequestParam(required = false) @Nullable Boolean active) {
        return articleService.getArticles(query, new ArticleFilter(type, rangeId, supplierId, active));
    }

    @GetMapping("/exportable")
    @Operation(summary = "Rows of the article export, same filters as the list; /exportable/export downloads the xlsx")
    @ApiResponse(responseCode = "200", description = "Page of export rows")
    @HasPermission("articles.export")
    @ExcelExport(target = "articles", filename = "articles", sheet = "Articles")
    public PageResponse<ArticleExportRDto> getExportableArticles(
            @Valid PageQuery query,
            @RequestParam(required = false) @Nullable ArticleType type,
            @RequestParam(required = false) @Nullable UUID rangeId,
            @RequestParam(required = false) @Nullable UUID supplierId,
            @RequestParam(required = false) @Nullable Boolean active) {
        return articleService.getExportableArticles(query, new ArticleFilter(type, rangeId, supplierId, active));
    }

    @GetMapping("/production")
    @Operation(summary = "Active articles usable in production: INPUT = MP/SF, OUTPUT = finished products (ART-04)")
    @ApiResponse(responseCode = "200", description = "Page of articles")
    @ApiResponse(responseCode = "400", description = "Missing or unknown usage (MALFORMED_REQUEST)")
    @HasPermission("articles.read")
    public PageResponse<ArticleRDto> getProductionArticles(
            @Valid PageQuery query, @RequestParam ProductionUsage usage) {
        return articleService.getProductionArticles(query, usage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Article sheet")
    @ApiResponse(responseCode = "200", description = "Article")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @HasPermission("articles.read")
    public ArticleRDto getArticleById(@PathVariable UUID id) {
        return articleService.getArticleById(id);
    }

    @PostMapping
    @Operation(summary = "Creates an article")
    @ApiResponse(responseCode = "201", description = "Article created")
    @ApiResponse(responseCode = "409", description = "Barcode already used (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("articles.create")
    public ResponseEntity<ArticleRDto> createArticle(@Valid @RequestBody ArticleWDto request) {
        var created = articleService.createArticle(request);
        return ResponseEntity.created(URI.create("/api/v1/articles/" + created.id()))
                .body(created);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Bulk import from a CSV file (UTF-8, ';' separator, 1000 rows max); partial: valid rows are kept")
    @ApiResponse(
            responseCode = "200",
            description = "Report with one line per row (created, updated, conflict, invalid)")
    @ApiResponse(
            responseCode = "422",
            description = "Unreadable, empty, too large or badly headed file (VALIDATION_FAILED)")
    @HasPermission("articles.import")
    public ArticleImportRDto importArticles(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "SKIP") ImportConflictMode onConflict,
            @RequestParam(defaultValue = "") Set<Integer> onlyRows) {
        return articleImportService.importArticles(file, onConflict, onlyRows);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates an article sheet")
    @ApiResponse(responseCode = "200", description = "Article updated")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Version or barcode conflict (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("articles.update")
    public ArticleRDto updateArticle(@PathVariable UUID id, @Valid @RequestBody ArticleWDto request) {
        return articleService.updateArticle(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activates or deactivates an article")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("articles.update")
    public ArticleRDto setActive(@PathVariable UUID id, @Valid @RequestBody ArticleStatusWDto request) {
        return articleService.setActive(id, request.active());
    }
}
