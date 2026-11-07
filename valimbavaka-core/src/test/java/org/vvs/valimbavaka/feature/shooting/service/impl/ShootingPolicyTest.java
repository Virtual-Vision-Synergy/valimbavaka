package org.vvs.valimbavaka.feature.shooting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingItemWDto;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingWDto;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;

@ExtendWith(MockitoExtension.class)
class ShootingPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ArticleService articleService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private AuthenticatedUserLoader authenticatedUserLoader;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private ShootingPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new ShootingPolicy(
                articleService,
                referentialService,
                authenticatedUserLoader,
                auditorAware,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void draft_validRequest_copiesArticlesAuthorAndTrimmedNote() {
        // Arrange
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article());
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(USER_ID));
        when(authenticatedUserLoader.findById(USER_ID)).thenReturn(Optional.of(user()));
        var request = request(NOW.plusSeconds(3600), "  Campagne de Noël  ", item(ARTICLE_ID, "2"));

        // Act
        var draft = sut.draft(request);

        // Assert
        assertThat(draft.locationId()).isEqualTo(LOCATION_ID);
        assertThat(draft.takenByName()).isEqualTo("Miora Andria");
        assertThat(draft.note()).isEqualTo("Campagne de Noël");
        assertThat(draft.items()).singleElement().satisfies(line -> {
            assertThat(line.getArticleCode()).isEqualTo("ART-0031");
            assertThat(line.getArticleName()).isEqualTo("Gommage café 200 g");
            assertThat(line.getQuantity()).isEqualByComparingTo("2");
        });
    }

    @Test
    void draft_blankNote_isStoredAsNull() {
        // Arrange
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(true);
        when(articleService.getArticleById(ARTICLE_ID)).thenReturn(article());
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(USER_ID));
        when(authenticatedUserLoader.findById(USER_ID)).thenReturn(Optional.of(user()));

        // Act
        var draft = sut.draft(request(NOW.plusSeconds(3600), "   ", item(ARTICLE_ID, "1")));

        // Assert
        assertThat(draft.note()).isNull();
    }

    @Test
    void draft_expectedReturnNotInTheFuture_throwsBusinessException() {
        // Arrange
        var request = request(NOW, null, item(ARTICLE_ID, "1"));

        // Act + Assert
        assertThatThrownBy(() -> sut.draft(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("retour prévu");
        verifyNoInteractions(articleService, referentialService);
    }

    @Test
    void draft_inactiveLocation_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(false);
        var request = request(NOW.plusSeconds(3600), null, item(ARTICLE_ID, "1"));

        // Act + Assert
        assertThatThrownBy(() -> sut.draft(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("emplacement");
        verifyNoInteractions(articleService);
    }

    @Test
    void draft_sameArticleTwice_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(true);
        var request = request(NOW.plusSeconds(3600), null, item(ARTICLE_ID, "1"), item(ARTICLE_ID, "2"));

        // Act + Assert
        assertThatThrownBy(() -> sut.draft(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("une fois");
        verifyNoInteractions(articleService);
    }

    private static ShootingWDto request(Instant expectedReturnAt, String note, ShootingItemWDto... items) {
        return new ShootingWDto(LOCATION_ID, List.of(items), expectedReturnAt, note);
    }

    private static ShootingItemWDto item(UUID articleId, String quantity) {
        return new ShootingItemWDto(articleId, new BigDecimal(quantity));
    }

    private static ArticleRDto article() {
        return new ArticleRDto(
                ARTICLE_ID,
                "ART-0031",
                "Gommage café 200 g",
                null,
                ArticleType.FINISHED,
                null,
                ArticleUnit.PIECE,
                null,
                null,
                BigDecimal.ZERO,
                null,
                true,
                NOW,
                NOW,
                0);
    }

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(
                USER_ID,
                "miora@valimbavaka.mg",
                "Miora",
                "Andria",
                "SELLER",
                "Vendeuse",
                "hash",
                Set.of("shootings.create"),
                true,
                false,
                null);
    }
}
