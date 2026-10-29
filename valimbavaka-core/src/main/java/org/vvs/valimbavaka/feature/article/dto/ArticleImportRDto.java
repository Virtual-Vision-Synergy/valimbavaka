package org.vvs.valimbavaka.feature.article.dto;

import java.util.List;

import org.vvs.valimbavaka.feature.article.domain.ImportRowStatus;

/** Report of a bulk import: one line per processed row, plus the counters shown to the administrator. */
public record ArticleImportRDto(
        int created, int updated, int unchanged, int conflicts, int invalid, List<ArticleImportRowRDto> rows) {

    public static ArticleImportRDto of(List<ArticleImportRowRDto> rows) {
        return new ArticleImportRDto(
                count(rows, ImportRowStatus.CREATED),
                count(rows, ImportRowStatus.UPDATED),
                count(rows, ImportRowStatus.UNCHANGED),
                count(rows, ImportRowStatus.CONFLICT),
                count(rows, ImportRowStatus.INVALID),
                List.copyOf(rows));
    }

    private static int count(List<ArticleImportRowRDto> rows, ImportRowStatus status) {
        return (int) rows.stream().filter(row -> row.status() == status).count();
    }
}
