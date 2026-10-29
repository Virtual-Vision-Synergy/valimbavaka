package org.vvs.valimbavaka.feature.article.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ImportConflictMode;
import org.vvs.valimbavaka.feature.article.domain.ImportRowStatus;
import org.vvs.valimbavaka.feature.article.dto.ArticleImportRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleImportRowRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleImportService;

@Service
@Transactional(readOnly = true)
public class ArticleImportServiceImpl implements ArticleImportService {

    private static final Logger log = LoggerFactory.getLogger(ArticleImportServiceImpl.class);

    private final ArticleCsvParser parser;
    private final ArticleImportRowProcessor rowProcessor;

    public ArticleImportServiceImpl(ArticleCsvParser parser, ArticleImportRowProcessor rowProcessor) {
        this.parser = parser;
        this.rowProcessor = rowProcessor;
    }

    /**
     * Not transactional on purpose ({@code NOT_SUPPORTED}): each row commits on its own in
     * {@link ArticleImportRowProcessor}, which is what makes the import partial.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArticleImportRDto importArticles(MultipartFile file, ImportConflictMode mode, Set<Integer> onlyRows) {
        var records = parser.parse(read(file));
        Map<String, Integer> seen = new HashMap<>();
        List<ArticleImportRowRDto> rows = new ArrayList<>();
        for (var record : records) {
            if (onlyRows.isEmpty() || onlyRows.contains(record.row())) {
                rows.add(processRow(record, mode, seen));
            }
        }
        var report = ArticleImportRDto.of(rows);
        log.info(
                "Import d'articles : {} créés, {} modifiés, {} conflits, {} invalides",
                report.created(),
                report.updated(),
                report.conflicts(),
                report.invalid());
        return report;
    }

    private ArticleImportRowRDto processRow(
            ArticleCsvRecord record, ImportConflictMode mode, Map<String, Integer> seen) {
        var repeated = findRepeat(record, seen);
        if (repeated != null) {
            return new ArticleImportRowRDto(record.row(), ImportRowStatus.CONFLICT, record.get("code"), repeated);
        }
        try {
            return rowProcessor.process(record, mode);
        } catch (DataAccessException e) {
            log.warn("Ligne {} de l'import refusée par la base : {}", record.row(), e.getMessage());
            return new ArticleImportRowRDto(
                    record.row(),
                    ImportRowStatus.CONFLICT,
                    record.get("code"),
                    "Conflit avec une écriture simultanée : relancez l'import de cette ligne.");
        }
    }

    /** A code or barcode met twice in the file: the first row wins, the following ones are reported. */
    private static @Nullable String findRepeat(ArticleCsvRecord record, Map<String, Integer> seen) {
        for (var column : List.of("code", "barcode")) {
            var value = record.get(column);
            if (value == null) {
                continue;
            }
            var first = seen.putIfAbsent(column + ":" + value, record.row());
            if (first != null) {
                return "Doublon dans le fichier : %s « %s » déjà présent à la ligne %d."
                        .formatted(column, value, first);
            }
        }
        return null;
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Le fichier n'a pas pu être lu.");
        }
    }
}
