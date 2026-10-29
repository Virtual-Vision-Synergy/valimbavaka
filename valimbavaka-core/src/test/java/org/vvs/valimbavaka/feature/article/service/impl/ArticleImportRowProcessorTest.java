package org.vvs.valimbavaka.feature.article.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.domain.ArticleDetails;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.domain.ImportConflictMode;
import org.vvs.valimbavaka.feature.article.domain.ImportRowStatus;
import org.vvs.valimbavaka.feature.article.repository.ArticleRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

@ExtendWith(MockitoExtension.class)
class ArticleImportRowProcessorTest {

    private static final String BARCODE = "3760001230014";

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private JournalService journalService;

    @Mock
    private ArticleReferenceChecker referenceChecker;

    private ArticleImportRowProcessor sut;

    @BeforeEach
    void setUp() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new ArticleImportRowProcessor(articleRepository, journalService, referenceChecker, validator);
    }

    @Test
    void process_newRowWithoutCode_createsWithGeneratedCodeAndJournals() {
        // Arrange
        when(articleRepository.nextCodeNumber()).thenReturn(31L);
        when(articleRepository.save(any(Article.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        var result = sut.process(row("name", "  Savon noir 100 g  ", "type", "RAW_MATERIAL"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.CREATED);
        assertThat(result.code()).isEqualTo("ART-0031");
        verify(journalService).record(eq(JournalAction.CREATE), eq("ARTICLE"), any(), anyString());
        verify(articleRepository, never()).advanceCodeSequence(anyLong());
    }

    @Test
    void process_newRowWithFileCode_createsWithThatCodeAndAdvancesSequence() {
        // Arrange
        when(articleRepository.findByCode("ART-0500")).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        var result = sut.process(row("code", "ART-0500"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.CREATED);
        assertThat(result.code()).isEqualTo("ART-0500");
        verify(articleRepository).advanceCodeSequence(500L);
        verify(articleRepository, never()).nextCodeNumber();
    }

    @Test
    void process_decimalWithComma_isAccepted() {
        // Arrange
        when(articleRepository.nextCodeNumber()).thenReturn(1L);
        when(articleRepository.save(any(Article.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        var result = sut.process(row("cost", "12,5", "threshold", "0,250"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.CREATED);
    }

    @Test
    void process_existingBarcodeInSkipMode_returnsConflictAndWritesNothing() {
        // Arrange
        when(articleRepository.findByBarcode(BARCODE)).thenReturn(Optional.of(existing("ART-0007", BARCODE)));

        // Act
        var result = sut.process(row("barcode", BARCODE), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.CONFLICT);
        assertThat(result.code()).isEqualTo("ART-0007");
        verify(articleRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void process_existingCodeInSkipMode_returnsConflict() {
        // Arrange
        when(articleRepository.findByCode("ART-0007")).thenReturn(Optional.of(existing("ART-0007", null)));

        // Act
        var result = sut.process(row("code", "ART-0007"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.CONFLICT);
        assertThat(result.message()).contains("ART-0007");
    }

    @Test
    void process_existingBarcodeInOverwriteMode_updatesArticleAndJournals() {
        // Arrange
        var article = existing("ART-0007", BARCODE);
        when(articleRepository.findByBarcode(BARCODE)).thenReturn(Optional.of(article));

        // Act
        var result = sut.process(row("barcode", BARCODE, "name", "Savon noir 250 g"), ImportConflictMode.OVERWRITE);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.UPDATED);
        assertThat(article.getName()).isEqualTo("Savon noir 250 g");
        verify(journalService).record(eq(JournalAction.UPDATE), eq("ARTICLE"), any(), anyString());
    }

    @Test
    void process_overwriteWithSameValues_returnsUnchangedWithoutJournal() {
        // Arrange
        var article = existing("ART-0007", BARCODE);
        when(articleRepository.findByCode("ART-0007")).thenReturn(Optional.of(article));
        when(articleRepository.findByBarcode(BARCODE)).thenReturn(Optional.of(article));

        // Act
        var result = sut.process(row("code", "ART-0007", "barcode", BARCODE), ImportConflictMode.OVERWRITE);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.UNCHANGED);
        verifyNoInteractions(journalService);
    }

    @Test
    void process_codeAndBarcodeOfTwoArticles_returnsConflictEvenInOverwriteMode() {
        // Arrange
        when(articleRepository.findByCode("ART-0007")).thenReturn(Optional.of(existing("ART-0007", null)));
        when(articleRepository.findByBarcode(BARCODE)).thenReturn(Optional.of(existing("ART-0008", BARCODE)));

        // Act
        var result = sut.process(row("code", "ART-0007", "barcode", BARCODE), ImportConflictMode.OVERWRITE);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.CONFLICT);
        assertThat(result.code()).isEqualTo("ART-0008");
        verify(articleRepository, never()).save(any());
    }

    @Test
    void process_unknownCodeWithBarcodeOfAnotherArticle_returnsConflict() {
        // Arrange
        when(articleRepository.findByCode("ART-0900")).thenReturn(Optional.empty());
        when(articleRepository.findByBarcode(BARCODE)).thenReturn(Optional.of(existing("ART-0008", BARCODE)));

        // Act
        var result = sut.process(row("code", "ART-0900", "barcode", BARCODE), ImportConflictMode.OVERWRITE);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.CONFLICT);
    }

    @Test
    void process_blankName_returnsInvalid() {
        // Act
        var result = sut.process(row("name", "   "), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.message()).isEqualTo("Le nom est obligatoire");
        verifyNoInteractions(articleRepository);
    }

    @Test
    void process_unknownType_returnsInvalid() {
        // Act
        var result = sut.process(row("type", "EMBALLAGE"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.message()).isEqualTo("Valeur inconnue pour type : « EMBALLAGE ».");
    }

    @Test
    void process_negativePriceAndShortBarcode_returnsInvalidWithBothMessages() {
        // Act
        var result = sut.process(row("price", "-1", "barcode", "123"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.message())
                .contains("Le prix ne peut pas être négatif")
                .contains("13 chiffres");
    }

    @Test
    void process_badCodeFormat_returnsInvalid() {
        // Act
        var result = sut.process(row("code", "ART-12"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.message()).isEqualTo("Code invalide : « ART-12 » (format attendu ART-0001).");
    }

    @Test
    void process_notANumber_returnsInvalid() {
        // Act
        var result = sut.process(row("threshold", "dix"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.message()).isEqualTo("Nombre invalide pour threshold : « dix ».");
    }

    @Test
    void process_finishedWithoutRange_returnsInvalidFromReferenceChecker() {
        // Arrange
        doThrow(new BusinessException("La gamme est obligatoire pour un produit fini."))
                .when(referenceChecker)
                .check(any(), any());

        // Act
        var result = sut.process(row("type", "FINISHED"), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.status()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(result.message()).isEqualTo("La gamme est obligatoire pour un produit fini.");
        verify(articleRepository, never()).save(any());
    }

    @Test
    void process_recordWithReadError_returnsInvalid() {
        // Act
        var result = sut.process(
                new ArticleCsvRecord(4, Map.of(), "Nombre de colonnes incorrect."), ImportConflictMode.SKIP);

        // Assert
        assertThat(result.row()).isEqualTo(4);
        assertThat(result.status()).isEqualTo(ImportRowStatus.INVALID);
    }

    /** A valid raw material row, with the given column/value pairs replacing the defaults. */
    private static ArticleCsvRecord row(String... overrides) {
        Map<String, String> values = new HashMap<>(
                Map.of("name", "Savon noir 100 g", "type", "RAW_MATERIAL", "unit", "PIECE", "threshold", "10"));
        for (int i = 0; i < overrides.length; i += 2) {
            values.put(overrides[i], overrides[i + 1]);
        }
        return new ArticleCsvRecord(1, values, null);
    }

    private static Article existing(String code, String barcode) {
        var details = new ArticleDetails(
                "Savon noir 100 g",
                barcode,
                ArticleType.RAW_MATERIAL,
                null,
                ArticleUnit.PIECE,
                null,
                null,
                new BigDecimal("10"),
                null);
        return Article.create(code, details);
    }
}
