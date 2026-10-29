package org.vvs.valimbavaka.feature.article.service;

import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import org.vvs.valimbavaka.feature.article.domain.ImportConflictMode;
import org.vvs.valimbavaka.feature.article.dto.ArticleImportRDto;

public interface ArticleImportService {

    /**
     * Imports a CSV file (UTF-8, {@code ;} separator, 1000 rows maximum). Valid rows are created even if others are
     * rejected (partial import). {@code onlyRows} (1-based row numbers) restricts the run to those rows, so that the
     * administrator can replay the conflicts with {@link ImportConflictMode#OVERWRITE}; empty = all rows.
     */
    ArticleImportRDto importArticles(MultipartFile file, ImportConflictMode mode, Set<Integer> onlyRows);
}
