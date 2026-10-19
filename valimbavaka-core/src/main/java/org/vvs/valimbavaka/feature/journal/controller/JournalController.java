package org.vvs.valimbavaka.feature.journal.controller;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.journal.dto.JournalEntryRDto;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Consultation seule : le journal est append-only, aucune route d'écriture, de modification ou de suppression. */
@Tag(name = "Journal")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Requête mal formée (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Non authentifié (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Permission manquante (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/journal")
@Validated
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping
    @Operation(summary = "Journal des opérations, du plus récent au plus ancien")
    @ApiResponse(responseCode = "200", description = "Page d'entrées du journal")
    @HasPermission("journal.read")
    public PageResponse<JournalEntryRDto> getEntries(@Valid PageQuery query) {
        return journalService.getEntries(query);
    }
}
