package org.vvs.valimbavaka.feature.article.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.domain.ArticleDetails;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.domain.ProductionUsage;
import org.vvs.valimbavaka.feature.article.mapper.ArticleMapper;
import org.vvs.valimbavaka.feature.article.repository.ArticleRepository;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

/** ART-04: production usage of an article, derived from its type (separate from ArticleServiceImplTest, 300 lines). */
@ExtendWith(MockitoExtension.class)
class ArticleProductionServiceTest {

    private static final UUID ARTICLE_ID = UUID.randomUUID();

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
    void getProductionArticles_input_searchesActiveRawMaterialsAndSemiFinished() {
        // Arrange
        when(articleRepository.searchActiveByTypes(eq("huile"), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        var result = sut.getProductionArticles(new PageQuery(1, 20, " huile ", null, null), ProductionUsage.INPUT);

        // Assert
        verify(articleRepository)
                .searchActiveByTypes(
                        eq("huile"),
                        eq(Set.of(ArticleType.RAW_MATERIAL, ArticleType.SEMI_FINISHED)),
                        any(Pageable.class));
        assertThat(result.items()).isEmpty();
    }

    @Test
    void getProductionArticles_output_searchesActiveFinishedProductsOnly() {
        // Arrange
        when(articleRepository.searchActiveByTypes(eq(""), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        sut.getProductionArticles(new PageQuery(1, 20, null, null, null), ProductionUsage.OUTPUT);

        // Assert
        verify(articleRepository)
                .searchActiveByTypes(eq(""), eq(Set.of(ArticleType.FINISHED)), any(Pageable.class));
    }

    @Test
    void requireProductionArticle_activeRawMaterialAsInput_returnsTheArticle() {
        // Arrange
        var article = article(ArticleType.RAW_MATERIAL);
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));

        // Act
        sut.requireProductionArticle(ARTICLE_ID, ProductionUsage.INPUT);

        // Assert
        verify(articleMapper).toRDto(article);
    }

    @Test
    void requireProductionArticle_activeFinishedProductAsOutput_returnsTheArticle() {
        // Arrange
        var article = article(ArticleType.FINISHED);
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));

        // Act
        sut.requireProductionArticle(ARTICLE_ID, ProductionUsage.OUTPUT);

        // Assert
        verify(articleMapper).toRDto(article);
    }

    @Test
    void requireProductionArticle_finishedProductAsInput_throwsValidationFailed() {
        // Arrange
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article(ArticleType.FINISHED)));

        // Act + Assert
        assertThatThrownBy(() -> sut.requireProductionArticle(ARTICLE_ID, ProductionUsage.INPUT))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Seules les matières premières et les semi-finis peuvent entrer en production.")
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void requireProductionArticle_semiFinishedAsOutput_throwsValidationFailed() {
        // Arrange
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article(ArticleType.SEMI_FINISHED)));

        // Act + Assert
        assertThatThrownBy(() -> sut.requireProductionArticle(ARTICLE_ID, ProductionUsage.OUTPUT))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Seuls les produits finis peuvent sortir de production.");
    }

    @Test
    void requireProductionArticle_inactiveRawMaterial_throwsValidationFailed() {
        // Arrange
        var article = article(ArticleType.RAW_MATERIAL);
        article.deactivate();
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(article));

        // Act + Assert
        assertThatThrownBy(() -> sut.requireProductionArticle(ARTICLE_ID, ProductionUsage.INPUT))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Article désactivé, inutilisable en production : ART-0042");
    }

    @Test
    void requireProductionArticle_unknownId_throwsNotFound() {
        // Arrange
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.requireProductionArticle(ARTICLE_ID, ProductionUsage.INPUT))
                .isInstanceOf(NotFoundException.class);
    }

    private static Article article(ArticleType type) {
        return Article.create(
                "ART-0042",
                new ArticleDetails(
                        "Huile de coco",
                        null,
                        type,
                        type == ArticleType.FINISHED ? UUID.randomUUID() : null,
                        ArticleUnit.KILOGRAM,
                        new BigDecimal("12000"),
                        null,
                        new BigDecimal("5"),
                        null));
    }
}
