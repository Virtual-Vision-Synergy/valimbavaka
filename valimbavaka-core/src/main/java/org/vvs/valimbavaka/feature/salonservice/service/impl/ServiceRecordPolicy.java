package org.vvs.valimbavaka.feature.salonservice.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.UnauthorizedException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft.Place;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft.Seller;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft.ServiceCopy;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordProduct;
import org.vvs.valimbavaka.feature.salonservice.dto.ConsumedProductWDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordWDto;
import org.vvs.valimbavaka.feature.salonservice.service.SalonCatalogService;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;

/**
 * Rules of a new performed service: active catalogue service, products (the recipe when none are sent) each listed
 * once, seller = authenticated account, salon stock location and store sales channel.
 */
@Component
class ServiceRecordPolicy {

    /** Stock location of the salon: code {@code TRADE_SHOW}, label « Salon » (SYS-05). */
    static final String SALON_LOCATION_CODE = "TRADE_SHOW";

    /** Sales channel of a performed service for the commission: the store (hypothesis, see the SRV-02 guide). */
    static final String SALON_CHANNEL_CODE = "STORE";

    private final SalonCatalogService catalogService;
    private final ArticleService articleService;
    private final ReferentialService referentialService;
    private final AuthenticatedUserLoader authenticatedUserLoader;
    private final AuditorAware<UUID> auditorAware;

    ServiceRecordPolicy(
            SalonCatalogService catalogService,
            ArticleService articleService,
            ReferentialService referentialService,
            AuthenticatedUserLoader authenticatedUserLoader,
            AuditorAware<UUID> auditorAware) {
        this.catalogService = catalogService;
        this.articleService = articleService;
        this.referentialService = referentialService;
        this.authenticatedUserLoader = authenticatedUserLoader;
        this.auditorAware = auditorAware;
    }

    /** Checks the request and copies the service and each article (404 if unknown) before anything is written. */
    ServiceRecordDraft draft(ServiceRecordWDto request) {
        var service = catalogService.getServiceById(request.salonServiceId());
        if (!service.active()) {
            throw new BusinessException("Le service salon est désactivé : choisissez un service actif.");
        }
        var requested = request.products();
        var lines = requested != null ? requested : recipe(service);
        var articleIds = new HashSet<UUID>();
        if (!lines.stream().map(ConsumedProductWDto::articleId).allMatch(articleIds::add)) {
            throw new BusinessException("Un produit ne peut figurer qu'une fois dans une prestation.");
        }
        List<ServiceRecordProduct> products = lines.stream().map(this::product).toList();
        var copy = new ServiceCopy(service.name(), service.durationMinutes(), service.price());
        return new ServiceRecordDraft(
                service.id(),
                copy,
                currentSeller(),
                place(),
                text(request.customerName()),
                text(request.note()),
                products);
    }

    private static List<ConsumedProductWDto> recipe(SalonServiceRDto service) {
        return service.products().stream()
                .map(product -> new ConsumedProductWDto(product.articleId(), product.quantity()))
                .toList();
    }

    private ServiceRecordProduct product(ConsumedProductWDto line) {
        var article = articleService.getArticleById(line.articleId());
        return ServiceRecordProduct.of(article.id(), article.code(), article.name(), line.quantity());
    }

    private Place place() {
        var channelId = referentialService
                .findSalesChannelId(SALON_CHANNEL_CODE)
                .orElseThrow(() -> new BusinessException("Le canal de vente boutique est introuvable."));
        var locationId = referentialService
                .findStockLocationId(SALON_LOCATION_CODE)
                .orElseThrow(() -> new BusinessException("L'emplacement de stock salon est introuvable."));
        return new Place(channelId, locationId);
    }

    private Seller currentSeller() {
        return auditorAware
                .getCurrentAuditor()
                .flatMap(authenticatedUserLoader::findById)
                .map(ServiceRecordPolicy::seller)
                .orElseThrow(() -> new UnauthorizedException("Une session active est requise."));
    }

    private static Seller seller(AuthenticatedUser user) {
        return new Seller(user.id(), user.firstName() + " " + user.lastName());
    }

    private static @Nullable String text(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
