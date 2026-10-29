package org.vvs.valimbavaka.feature.article.dto;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ImportRowStatus;

/**
 * Result of one data row. {@code row} is the 1-based number of the row after the header; {@code code} is the code of
 * the article created, updated or in conflict (null when unknown); {@code message} explains a rejection.
 */
public record ArticleImportRowRDto(
        int row,
        ImportRowStatus status,
        @Nullable String code,
        @Nullable String message) {}
