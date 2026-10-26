package org.vvs.valimbavaka.feature.article.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.domain.ArticleDetails;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.feature.article.mapper.ArticleMapper;
import org.vvs.valimbavaka.feature.article.repository.ArticleRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final UUID RANGE_ID = UUID.randomUUID();
    private static final String BARCODE = "3760123456789";

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private JournalService journalService;

    @Mock
    private ArticleReferenceChecker referenceChecker;

    private ArticleServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ArticleServiceImpl(articleRepository, articleMapper, journalService, referenceChecker);
    }

    @Test
    void createArticle_validFinishedProduct_savesWithServerCodeAndTracesInJournal() {
        // Arrange
        when(articleRepository.nextCodeNumber()).thenReturn(31L);
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sut.createArticle(request("  Savon noir 100 g ", ArticleType.FINISHED, null));

        // Assert
        var saved = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("ART-0031");
        assertThat(saved.getValue().getName()).isEqualTo("Savon noir 100 g");
        assertThat(saved.getValue().isActive()).isTrue();
        verify(referenceChecker).check(any(ArticleWDto.class), isNull());
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("ARTICLE"),
                        any(),
                        eq("Création de l'article ART-0031 · Savon noir 100 g"));
    }

    @Test
    void createArticle_barcodeAlreadyUsed_throwsConflictWithoutSaving() {
        // Arrange
        when(articleRepository.existsByBarcode(BARCODE)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.createArticle(request("Savon noir 100 g", ArticleType.FINISHED, BARCODE)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Un article avec ce code-barres existe déjà.");
        verify(articleRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void createArticle_referenceRefused_savesNothing() {
        // Arrange
        doThrow(new BusinessException("La gamme est obligatoire pour un produit fini."))
                .when(referenceChecker)
                .check(any(ArticleWDto.class), any());

        // Act + Assert
        assertThatThrownBy(() -> sut.createArticle(request("Savon noir 100 g", ArticleType.FINISHED, null)))
                .isInstanceOf(BusinessException.class);
        verify(articleRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void getArticleById_unknownId_throwsNotFound() {
        // Arrange
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getArticleById(ARTICLE_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateArticle_changedFields_appliesThemAndTracesInJournal() {
        // Arrange
        var article = existingArticle();
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));
        var request = new ArticleWDto(
                "Savon noir 120 g", null, ArticleType.FINISHED, RANGE_ID, ArticleUnit.PIECE,
                new BigDecimal("3300"), new BigDecimal("9000"), new BigDecimal("25"), null, null);

        // Act
        sut.updateArticle(ARTICLE_ID, request);

        // Assert
        assertThat(article.getName()).isEqualTo("Savon noir 120 g");
        assertThat(article.getPrice()).isEqualByComparingTo("9000");
        assertThat(article.getThreshold()).isEqualByComparingTo("25");
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "ARTICLE",
                        ARTICLE_ID,
                        "Modification de l'article ART-0031 · Savon noir 120 g");
    }

    @Test
    void updateArticle_sameValuesWithOtherSpacing_leavesNoTrace() {
        // Arrange
        var article = existingArticle();
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));

        // Act
        sut.updateArticle(ARTICLE_ID, request("  Savon noir 100 g ", ArticleType.FINISHED, null));

        // Assert
        verifyNoInteractions(journalService);
    }

    @Test
    void updateArticle_staleVersion_throwsConflictAndChangesNothing() {
        // Arrange
        var article = existingArticle();
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));
        var request = new ArticleWDto(
                "Autre nom", null, ArticleType.FINISHED, RANGE_ID, ArticleUnit.PIECE,
                null, new BigDecimal("8500"), BigDecimal.TEN, null, 42L);

        // Act + Assert
        assertThatThrownBy(() -> sut.updateArticle(ARTICLE_ID, request)).isInstanceOf(ConflictException.class);
        assertThat(article.getName()).isEqualTo("Savon noir 100 g");
        verifyNoInteractions(journalService);
    }

    @Test
    void updateArticle_barcodeOfAnotherArticle_throwsConflict() {
        // Arrange
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(existingArticle()));
        when(articleRepository.existsByBarcodeAndIdNot(BARCODE, ARTICLE_ID)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() ->
                        sut.updateArticle(ARTICLE_ID, request("Savon noir 100 g", ArticleType.FINISHED, BARCODE)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(journalService);
    }

    @Test
    void updateArticle_unknownId_throwsNotFound() {
        // Arrange
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() ->
                        sut.updateArticle(ARTICLE_ID, request("Savon noir 100 g", ArticleType.FINISHED, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void setActive_activeArticleDeactivated_deactivatesAndTracesInJournal() {
        // Arrange
        var article = existingArticle();
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));

        // Act
        sut.setActive(ARTICLE_ID, false);

        // Assert
        assertThat(article.isActive()).isFalse();
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "ARTICLE",
                        ARTICLE_ID,
                        "Désactivation de l'article ART-0031 · Savon noir 100 g");
    }

    @Test
    void setActive_stateAlreadyRequested_leavesNoTrace() {
        // Arrange
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(existingArticle()));

        // Act
        sut.setActive(ARTICLE_ID, true);

        // Assert
        verifyNoInteractions(journalService);
    }

    private static Article existingArticle() {
        return Article.create("ART-0031", new ArticleDetails(
                "Savon noir 100 g", null, ArticleType.FINISHED, RANGE_ID, ArticleUnit.PIECE,
                new BigDecimal("3120"), new BigDecimal("8500"), new BigDecimal("40"), null));
    }

    private static ArticleWDto request(String name, ArticleType type, String barcode) {
        return new ArticleWDto(
                name, barcode, type, RANGE_ID, ArticleUnit.PIECE,
                new BigDecimal("3120"), new BigDecimal("8500"), new BigDecimal("40"), null, null);
    }
}
