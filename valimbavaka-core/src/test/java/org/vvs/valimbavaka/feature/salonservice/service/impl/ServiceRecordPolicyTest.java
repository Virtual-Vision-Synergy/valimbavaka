package org.vvs.valimbavaka.feature.salonservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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
import org.vvs.valimbavaka.core.exception.UnauthorizedException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.salonservice.dto.ConsumedProductRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ConsumedProductWDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordWDto;
import org.vvs.valimbavaka.feature.salonservice.service.SalonCatalogService;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;

@ExtendWith(MockitoExtension.class)
class ServiceRecordPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();
    private static final UUID MASK_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID SALON_ID = UUID.randomUUID();

    @Mock
    private SalonCatalogService catalogService;

    @Mock
    private ArticleService articleService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private AuthenticatedUserLoader authenticatedUserLoader;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private ServiceRecordPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new ServiceRecordPolicy(
                catalogService, articleService, referentialService, authenticatedUserLoader, auditorAware);
    }

    @Test
    void draft_withoutProducts_consumesCatalogRecipeFromSalonWithStoreChannel() {
        // Arrange
        when(catalogService.getServiceById(SERVICE_ID)).thenReturn(service(true));
        stubArticles();
        stubSellerAndPlace();

        // Act
        var draft = sut.draft(new ServiceRecordWDto(SERVICE_ID, "  Hanta  ", null, "   "));

        // Assert
        assertThat(draft.service().name()).isEqualTo("Soin visage");
        assertThat(draft.service().price()).isEqualByComparingTo("35000");
        assertThat(draft.service().durationMinutes()).isEqualTo(45);
        assertThat(draft.seller().id()).isEqualTo(USER_ID);
        assertThat(draft.seller().name()).isEqualTo("Miora Andria");
        assertThat(draft.place().salesChannelId()).isEqualTo(STORE_ID);
        assertThat(draft.place().locationId()).isEqualTo(SALON_ID);
        assertThat(draft.customerName()).isEqualTo("Hanta");
        assertThat(draft.note()).isNull();
        var products = draft.products().stream()
                .map(product -> product.getArticleCode() + " " + product.getQuantity().toPlainString())
                .toList();
        assertThat(products).containsExactly("ART-0001 0.015", "ART-0002 1");
    }

    @Test
    void draft_withAdjustedProducts_usesSentQuantitiesInsteadOfRecipe() {
        // Arrange
        when(catalogService.getServiceById(SERVICE_ID)).thenReturn(service(true));
        when(articleService.getArticleById(OIL_ID)).thenReturn(article(OIL_ID, "ART-0001", "Huile de coco"));
        stubSellerAndPlace();
        var request = new ServiceRecordWDto(SERVICE_ID, null, List.of(line(OIL_ID, "0.020")), null);

        // Act
        var draft = sut.draft(request);

        // Assert
        assertThat(draft.customerName()).isNull();
        assertThat(draft.products()).singleElement().satisfies(product -> {
            assertThat(product.getArticleId()).isEqualTo(OIL_ID);
            assertThat(product.getArticleName()).isEqualTo("Huile de coco");
            assertThat(product.getQuantity()).isEqualByComparingTo("0.020");
        });
    }

    @Test
    void draft_emptyProducts_consumesNothing() {
        // Arrange
        when(catalogService.getServiceById(SERVICE_ID)).thenReturn(service(true));
        stubSellerAndPlace();

        // Act
        var draft = sut.draft(new ServiceRecordWDto(SERVICE_ID, null, List.of(), null));

        // Assert
        assertThat(draft.products()).isEmpty();
        verifyNoInteractions(articleService);
    }

    @Test
    void draft_inactiveService_throwsBusinessException() {
        // Arrange
        when(catalogService.getServiceById(SERVICE_ID)).thenReturn(service(false));

        // Act + Assert
        assertThatThrownBy(() -> sut.draft(new ServiceRecordWDto(SERVICE_ID, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivé");
        verifyNoInteractions(articleService, referentialService);
    }

    @Test
    void draft_sameArticleTwice_throwsBusinessException() {
        // Arrange
        when(catalogService.getServiceById(SERVICE_ID)).thenReturn(service(true));
        var products = List.of(line(OIL_ID, "0.010"), line(OIL_ID, "0.005"));

        // Act + Assert
        assertThatThrownBy(() -> sut.draft(new ServiceRecordWDto(SERVICE_ID, null, products, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("une fois");
        verifyNoInteractions(articleService);
    }

    @Test
    void draft_withoutSession_throwsUnauthorizedException() {
        // Arrange
        when(catalogService.getServiceById(SERVICE_ID)).thenReturn(service(true));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.draft(new ServiceRecordWDto(SERVICE_ID, null, List.of(), null)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void draft_salonLocationMissing_throwsBusinessException() {
        // Arrange
        when(catalogService.getServiceById(SERVICE_ID)).thenReturn(service(true));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(USER_ID));
        when(authenticatedUserLoader.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(referentialService.findSalesChannelId(ServiceRecordPolicy.SALON_CHANNEL_CODE))
                .thenReturn(Optional.of(STORE_ID));
        when(referentialService.findStockLocationId(ServiceRecordPolicy.SALON_LOCATION_CODE))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.draft(new ServiceRecordWDto(SERVICE_ID, null, List.of(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("salon");
    }

    private void stubArticles() {
        when(articleService.getArticleById(OIL_ID)).thenReturn(article(OIL_ID, "ART-0001", "Huile de coco"));
        when(articleService.getArticleById(MASK_ID)).thenReturn(article(MASK_ID, "ART-0002", "Masque argile"));
    }

    private void stubSellerAndPlace() {
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(USER_ID));
        when(authenticatedUserLoader.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(referentialService.findSalesChannelId(ServiceRecordPolicy.SALON_CHANNEL_CODE))
                .thenReturn(Optional.of(STORE_ID));
        when(referentialService.findStockLocationId(ServiceRecordPolicy.SALON_LOCATION_CODE))
                .thenReturn(Optional.of(SALON_ID));
    }

    private static ConsumedProductWDto line(UUID articleId, String quantity) {
        return new ConsumedProductWDto(articleId, new BigDecimal(quantity));
    }

    private static SalonServiceRDto service(boolean active) {
        var recipe = List.of(
                new ConsumedProductRDto(OIL_ID, new BigDecimal("0.015")),
                new ConsumedProductRDto(MASK_ID, new BigDecimal("1")));
        return new SalonServiceRDto(
                SERVICE_ID, "Soin visage", 45, new BigDecimal("35000"), null, recipe, active, NOW, NOW, 0);
    }

    private static ArticleRDto article(UUID id, String code, String name) {
        return new ArticleRDto(
                id,
                code,
                name,
                null,
                ArticleType.RAW_MATERIAL,
                null,
                ArticleUnit.LITER,
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
                Set.of("service-records.create"),
                true,
                false,
                null);
    }
}
