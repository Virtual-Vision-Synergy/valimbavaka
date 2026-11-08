package org.vvs.valimbavaka.feature.promotion.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;

@ExtendWith(MockitoExtension.class)
class PromotionRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @Mock
    private ReferentialService referentialService;

    @Mock
    private ArticleService articleService;

    private PromotionRules sut;

    @BeforeEach
    void setUp() {
        sut = new PromotionRules(referentialService, articleService);
    }

    @Test
    void check_percentOf100OnWholeCatalogueEndingToday_passes() {
        // Arrange
        when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(true);
        var details = details(PromotionKind.PERCENT, "100", period(-3, 0));

        // Act + Assert
        assertThatCode(() -> sut.check(details, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void check_percentAbove100_throwsBusinessException() {
        // Arrange
        var details = details(PromotionKind.PERCENT, "100.01", period(1, 30));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(details, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100");
    }

    @Test
    void check_buyXNotWholeOrBelow2OrAbove100_throwsBusinessException() {
        for (var value : List.of("2.5", "1", "101")) {
            // Arrange
            var details = details(PromotionKind.BUY_X, value, period(1, 30));

            // Act + Assert
            assertThatThrownBy(() -> sut.check(details, TODAY))
                    .as("valeur %s", value)
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("entre 2 et 100");
        }
    }

    @Test
    void check_buyX3WithTrailingZeros_passes() {
        // Arrange
        when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(true);
        var details = details(PromotionKind.BUY_X, "3.00", period(1, 30));

        // Act + Assert
        assertThatCode(() -> sut.check(details, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void check_endBeforeStart_throwsBusinessException() {
        // Arrange
        var details = details(PromotionKind.AMOUNT, "5000", period(10, 9));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(details, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("date de fin");
        verifyNoInteractions(referentialService, articleService);
    }

    @Test
    void check_periodAlreadyOverYesterday_throwsBusinessException() {
        // Arrange
        var details = details(PromotionKind.AMOUNT, "5000", period(-10, -1));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(details, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà terminée");
    }

    @Test
    void check_inactiveChannel_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(false);
        var details = details(PromotionKind.PERCENT, "15", period(1, 30));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(details, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Canal de vente");
    }

    @Test
    void check_wholeCatalogueWithTargets_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(true);
        var details = targeted(PromotionScope.ALL, Set.of(TARGET_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(details, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tout le catalogue");
    }

    @Test
    void check_rangesWithoutTarget_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(true);
        var details = targeted(PromotionScope.RANGES, Set.of());

        // Act + Assert
        assertThatThrownBy(() -> sut.check(details, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("au moins une gamme");
    }

    @Test
    void check_unknownRange_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(true);
        when(referentialService.isActiveProductRange(TARGET_ID)).thenReturn(false);
        var details = targeted(PromotionScope.RANGES, Set.of(TARGET_ID));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(details, TODAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Gamme introuvable");
    }

    @Test
    void check_activeFinishedArticle_passes() {
        // Arrange
        when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(true);
        when(articleService.getArticlesByIds(any())).thenReturn(List.of(article(ArticleType.FINISHED, true)));
        var details = targeted(PromotionScope.ARTICLES, Set.of(TARGET_ID));

        // Act + Assert
        assertThatCode(() -> sut.check(details, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void check_rawMaterialInactiveOrUnknownArticle_throwsBusinessException() {
        var rawMaterial = article(ArticleType.RAW_MATERIAL, true);
        var refused = Arrays.asList(rawMaterial, article(ArticleType.FINISHED, false), null);
        for (var found : refused) {
            // Arrange
            when(referentialService.isActiveSalesChannel(CHANNEL_ID)).thenReturn(true);
            when(articleService.getArticlesByIds(any())).thenReturn(found == null ? List.of() : List.of(found));
            var details = targeted(PromotionScope.ARTICLES, Set.of(TARGET_ID));

            // Act + Assert
            assertThatThrownBy(() -> sut.check(details, TODAY))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Article introuvable, désactivé ou non vendu");
        }
    }

    private static PromotionPeriod period(int startOffset, int endOffset) {
        return new PromotionPeriod(TODAY.plusDays(startOffset), TODAY.plusDays(endOffset));
    }

    private static PromotionDetails details(PromotionKind kind, String value, PromotionPeriod period) {
        return new PromotionDetails(
                "Octobre rose",
                null,
                kind,
                new BigDecimal(value),
                null,
                PromotionScope.ALL,
                Set.of(),
                period,
                Set.of(CHANNEL_ID),
                null);
    }

    /** 15 % from tomorrow for 30 days on the given targets. */
    private static PromotionDetails targeted(PromotionScope scope, Set<UUID> targets) {
        return new PromotionDetails(
                "Octobre rose",
                null,
                PromotionKind.PERCENT,
                new BigDecimal("15"),
                null,
                scope,
                targets,
                period(1, 30),
                Set.of(CHANNEL_ID),
                null);
    }

    private static ArticleRDto article(ArticleType type, boolean active) {
        return new ArticleRDto(
                TARGET_ID,
                "PF-0001",
                "Savon Botanika 100 g",
                null,
                type,
                null,
                ArticleUnit.PIECE,
                null,
                new BigDecimal("8500"),
                BigDecimal.ZERO,
                null,
                active,
                Instant.now(),
                Instant.now(),
                0L);
    }
}
