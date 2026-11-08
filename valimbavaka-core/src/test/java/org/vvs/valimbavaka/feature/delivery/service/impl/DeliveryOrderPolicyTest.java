package org.vvs.valimbavaka.feature.delivery.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderLineWDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderWDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderPolicyTest {

    /** 2026-11-08 at 22:30 UTC is already 2026-11-09 at 01:30 in Antananarivo (UTC+3). */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T22:30:00Z"), ZoneOffset.UTC);

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();

    @Mock
    private UserService userService;

    @Mock
    private ArticleService articleService;

    private DeliveryOrderPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new DeliveryOrderPolicy(userService, articleService, CLOCK);
    }

    @Test
    void check_validRequest_stripsTheCustomerAndPricesTheLinesFromTheArticle() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article(ArticleType.FINISHED, true, "8500.00"));
        var request = request(TODAY, "  Ivandry  ", "   ", line(ARTICLE_ID, "2"));

        // Act
        var command = sut.check(request);

        // Assert
        assertThat(command.customer().name()).isEqualTo("Rakoto Be");
        assertThat(command.customer().phone()).isEqualTo("034 12 345 67");
        assertThat(command.customer().district()).isEqualTo("Ivandry");
        assertThat(command.note()).isNull();
        assertThat(command.lines()).singleElement().satisfies(line -> {
            assertThat(line.articleId()).isEqualTo(ARTICLE_ID);
            assertThat(line.quantity()).isEqualByComparingTo("2");
            assertThat(line.unitPrice()).isEqualByComparingTo("8500.00");
        });
    }

    @Test
    void check_todayInAntananarivoWhileStillYesterdayInUtc_isAccepted() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article(ArticleType.FINISHED, true, "8500.00"));

        // Act
        var command = sut.check(request(TODAY, null, null, line(ARTICLE_ID, "1")));

        // Assert
        assertThat(command.scheduledDate()).isEqualTo(TODAY);
    }

    @Test
    void check_yesterday_throwsBusinessException() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(TODAY.minusDays(1), null, null, line(ARTICLE_ID, "1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La date de livraison ne peut pas être dans le passé.");
        verifyNoInteractions(articleService);
    }

    @Test
    void check_driverInactiveOrNotADriver_throwsBusinessException() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(TODAY, null, null, line(ARTICLE_ID, "1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("livreur");
        verifyNoInteractions(articleService);
    }

    @Test
    void check_sameArticleTwice_throwsBusinessException() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article(ArticleType.FINISHED, true, "8500.00"));
        var request = request(TODAY, null, null, line(ARTICLE_ID, "1"), line(ARTICLE_ID, "2"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un article ne peut figurer qu'une fois dans une commande.");
    }

    @Test
    void check_rawMaterial_throwsBusinessException() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article(ArticleType.RAW_MATERIAL, true, null));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(TODAY, null, null, line(ARTICLE_ID, "1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ART-0031");
    }

    @Test
    void check_inactiveFinishedProduct_throwsBusinessException() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article(ArticleType.FINISHED, false, "8500.00"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(TODAY, null, null, line(ARTICLE_ID, "1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("produit fini actif");
    }

    @Test
    void check_unknownArticle_propagatesNotFound() {
        // Arrange
        when(userService.isActiveDriver(DRIVER_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenThrow(new NotFoundException("Article introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(TODAY, null, null, line(ARTICLE_ID, "1"))))
                .isInstanceOf(NotFoundException.class);
    }

    private static DeliveryOrderLineWDto line(UUID articleId, String quantity) {
        return new DeliveryOrderLineWDto(articleId, new BigDecimal(quantity));
    }

    private static DeliveryOrderWDto request(
            LocalDate day, @Nullable String district, @Nullable String note, DeliveryOrderLineWDto... lines) {
        return new DeliveryOrderWDto(
                "  Rakoto Be ",
                " 034 12 345 67 ",
                "Lot II J 45, rue Pasteur",
                district,
                DRIVER_ID,
                day,
                List.of(lines),
                note);
    }

    private static ArticleRDto article(ArticleType type, boolean active, @Nullable String price) {
        var instant = Instant.parse("2026-11-08T08:00:00Z");
        return new ArticleRDto(
                ARTICLE_ID,
                "ART-0031",
                "Savon noir 100 g",
                null,
                type,
                null,
                ArticleUnit.PIECE,
                null,
                price == null ? null : new BigDecimal(price),
                BigDecimal.TEN,
                null,
                active,
                instant,
                instant,
                0);
    }
}
