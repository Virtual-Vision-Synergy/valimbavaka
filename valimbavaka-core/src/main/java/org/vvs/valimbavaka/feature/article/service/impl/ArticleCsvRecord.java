package org.vvs.valimbavaka.feature.article.service.impl;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * One data row of the import file, read by column name. {@code row} is the 1-based number after the header;
 * {@code error} is set when the row cannot even be read (wrong number of columns).
 */
record ArticleCsvRecord(
        int row, Map<String, String> values, @Nullable String error) {

    /** The trimmed cell, or null when the column is absent or blank. */
    @Nullable
    String get(String column) {
        var value = values.get(column);
        return value == null || value.isBlank() ? null : value.strip();
    }
}
