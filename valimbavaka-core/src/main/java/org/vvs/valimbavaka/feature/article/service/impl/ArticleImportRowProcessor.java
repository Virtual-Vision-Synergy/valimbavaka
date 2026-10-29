package org.vvs.valimbavaka.feature.article.service.impl;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.domain.ArticleDetails;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.domain.ImportConflictMode;
import org.vvs.valimbavaka.feature.article.domain.ImportRowStatus;
import org.vvs.valimbavaka.feature.article.dto.ArticleImportRowRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.feature.article.repository.ArticleRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

/**
 * Writes one row of the import in its own transaction, so that a rejected row never cancels the valid ones. Same
 * rules as the sheet ({@link ArticleServiceImpl}): Bean Validation, cross-feature checks, unique barcode, journal.
 */
@Component
@Transactional
public class ArticleImportRowProcessor {

    private static final Pattern CODE_FORMAT = Pattern.compile("^ART-(\\d{4,9})$");
    private static final String RESOURCE_TYPE = "ARTICLE";

    private final ArticleRepository articleRepository;
    private final JournalService journalService;
    private final ArticleReferenceChecker referenceChecker;
    private final Validator validator;

    ArticleImportRowProcessor(
            ArticleRepository articleRepository,
            JournalService journalService,
            ArticleReferenceChecker referenceChecker,
            Validator validator) {
        this.articleRepository = articleRepository;
        this.journalService = journalService;
        this.referenceChecker = referenceChecker;
        this.validator = validator;
    }

    ArticleImportRowRDto process(ArticleCsvRecord record, ImportConflictMode mode) {
        var readError = record.error();
        if (readError != null) {
            return rejected(record.row(), ImportRowStatus.INVALID, null, readError);
        }
        var code = record.get("code");
        try {
            var dto = toDto(record);
            checkFormat(code, dto);
            return write(record.row(), code, dto, mode);
        } catch (BusinessException e) {
            return rejected(record.row(), ImportRowStatus.INVALID, code, e.getMessage());
        }
    }

    private void checkFormat(@Nullable String code, ArticleWDto dto) {
        if (code != null && !CODE_FORMAT.matcher(code).matches()) {
            throw new BusinessException("Code invalide : « %s » (format attendu ART-0001).".formatted(code));
        }
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException(violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(" ")));
        }
    }

    private ArticleImportRowRDto write(int row, @Nullable String code, ArticleWDto dto, ImportConflictMode mode) {
        var barcode = dto.barcode();
        Optional<Article> byCode = code == null ? Optional.empty() : articleRepository.findByCode(code);
        Optional<Article> byBarcode = barcode == null ? Optional.empty() : articleRepository.findByBarcode(barcode);
        return byBarcode
                .filter(owner -> code != null && isOtherArticle(byCode.orElse(null), owner))
                .map(owner -> rejected(
                        row,
                        ImportRowStatus.CONFLICT,
                        owner.getCode(),
                        "Le code-barres %s appartient déjà à %s, qui n'est pas l'article du code %s."
                                .formatted(barcode, owner.getCode(), code)))
                .orElseGet(() -> byCode.or(() -> byBarcode)
                        .map(existing -> replace(row, existing, dto, mode))
                        .orElseGet(() -> create(row, code, dto)));
    }

    /** True when the file names a code that is unknown or belongs to another article than the barcode owner. */
    private static boolean isOtherArticle(@Nullable Article codeOwner, Article barcodeOwner) {
        return codeOwner == null || !codeOwner.getCode().equals(barcodeOwner.getCode());
    }

    private ArticleImportRowRDto create(int row, @Nullable String code, ArticleWDto dto) {
        referenceChecker.check(dto, null);
        var finalCode = code != null ? code : "ART-%04d".formatted(articleRepository.nextCodeNumber());
        if (code != null) {
            articleRepository.advanceCodeSequence(codeNumber(finalCode));
        }
        var saved = articleRepository.save(Article.create(finalCode, toDetails(dto)));
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                saved.getId(),
                "Création de l'article " + label(saved) + " (import)");
        return new ArticleImportRowRDto(row, ImportRowStatus.CREATED, finalCode, null);
    }

    /** Number of a code already matched against {@link #CODE_FORMAT} (4 to 9 digits, so it always fits a long). */
    private static long codeNumber(String code) {
        var matcher = CODE_FORMAT.matcher(code);
        try {
            if (matcher.matches()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (NumberFormatException e) {
            // falls through to the business error below
        }
        throw new BusinessException("Code invalide : « %s » (format attendu ART-0001).".formatted(code));
    }

    private ArticleImportRowRDto replace(int row, Article existing, ArticleWDto dto, ImportConflictMode mode) {
        if (mode == ImportConflictMode.SKIP) {
            return rejected(
                    row,
                    ImportRowStatus.CONFLICT,
                    existing.getCode(),
                    "L'article %s · %s existe déjà (même code ou même code-barres)."
                            .formatted(existing.getCode(), existing.getName()));
        }
        referenceChecker.check(dto, existing.getSupplierId());
        if (!existing.update(toDetails(dto))) {
            return new ArticleImportRowRDto(row, ImportRowStatus.UNCHANGED, existing.getCode(), null);
        }
        journalService.record(
                JournalAction.UPDATE,
                RESOURCE_TYPE,
                existing.getId(),
                "Modification de l'article " + label(existing) + " (import)");
        return new ArticleImportRowRDto(row, ImportRowStatus.UPDATED, existing.getCode(), null);
    }

    private static ArticleImportRowRDto rejected(
            int row, ImportRowStatus status, @Nullable String code, String message) {
        return new ArticleImportRowRDto(row, status, code, message);
    }

    private static String label(Article article) {
        return article.getCode() + " · " + article.getName();
    }

    private static ArticleWDto toDto(ArticleCsvRecord record) {
        return new ArticleWDto(
                orEmpty(record.get("name")),
                record.get("barcode"),
                enumValue(ArticleType.class, record, "type"),
                uuid(record, "rangeId"),
                enumValue(ArticleUnit.class, record, "unit"),
                decimal(record, "cost"),
                decimal(record, "price"),
                decimal(record, "threshold"),
                uuid(record, "supplierId"),
                null);
    }

    private static ArticleDetails toDetails(ArticleWDto dto) {
        return new ArticleDetails(
                dto.name(),
                dto.barcode(),
                dto.type(),
                dto.rangeId(),
                dto.unit(),
                dto.cost(),
                dto.price(),
                dto.threshold(),
                dto.supplierId());
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static <E extends Enum<E>> @Nullable E enumValue(Class<E> type, ArticleCsvRecord record, String column) {
        var raw = record.get(column);
        if (raw == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Valeur inconnue pour %s : « %s ».".formatted(column, raw));
        }
    }

    private static @Nullable UUID uuid(ArticleCsvRecord record, String column) {
        var raw = record.get(column);
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Identifiant invalide pour %s : « %s ».".formatted(column, raw));
        }
    }

    /** Accepts {@code 12.50} and {@code 12,50} (decimal comma of a spreadsheet in French). */
    private static @Nullable BigDecimal decimal(ArticleCsvRecord record, String column) {
        var raw = record.get(column);
        try {
            return raw == null ? null : new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BusinessException("Nombre invalide pour %s : « %s ».".formatted(column, raw));
        }
    }
}
