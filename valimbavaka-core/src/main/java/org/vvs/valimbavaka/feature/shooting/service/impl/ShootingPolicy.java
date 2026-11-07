package org.vvs.valimbavaka.feature.shooting.service.impl;

import java.time.Clock;
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
import org.vvs.valimbavaka.feature.shooting.domain.Shooting;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingDraft;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingItem;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingItemWDto;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingWDto;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;

/**
 * Rules of a new exit (return in the future, active location, each article once), of its return (now, by the
 * authenticated account) and the late computation.
 */
@Component
class ShootingPolicy {

    private final ArticleService articleService;
    private final ReferentialService referentialService;
    private final AuthenticatedUserLoader authenticatedUserLoader;
    private final AuditorAware<UUID> auditorAware;
    private final Clock clock;

    ShootingPolicy(
            ArticleService articleService,
            ReferentialService referentialService,
            AuthenticatedUserLoader authenticatedUserLoader,
            AuditorAware<UUID> auditorAware,
            Clock clock) {
        this.articleService = articleService;
        this.referentialService = referentialService;
        this.authenticatedUserLoader = authenticatedUserLoader;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    /** Checks the request and copies each article (404 if unknown) before anything is written. */
    ShootingDraft draft(ShootingWDto request) {
        if (!request.expectedReturnAt().isAfter(clock.instant())) {
            throw new BusinessException("Le retour prévu doit être postérieur à maintenant.");
        }
        if (!referentialService.isActiveStockLocation(request.locationId())) {
            throw new BusinessException("L'emplacement de stock est inconnu ou inactif.");
        }
        var articleIds = new HashSet<UUID>();
        if (!request.items().stream().map(ShootingItemWDto::articleId).allMatch(articleIds::add)) {
            throw new BusinessException("Un article ne peut figurer qu'une fois dans une sortie.");
        }
        List<ShootingItem> items = request.items().stream().map(this::item).toList();
        return new ShootingDraft(
                request.locationId(), currentUserName(), request.expectedReturnAt(), note(request.note()), items);
    }

    /** Return recorded now, by the authenticated account (409 if the exit is already returned). */
    void markReturned(Shooting shooting) {
        shooting.markReturned(clock.instant(), currentUserName());
    }

    boolean isLate(Shooting shooting) {
        return shooting.isLateAt(clock.instant());
    }

    private ShootingItem item(ShootingItemWDto line) {
        var article = articleService.getArticleById(line.articleId());
        return ShootingItem.of(article.id(), article.code(), article.name(), line.quantity());
    }

    private String currentUserName() {
        return auditorAware
                .getCurrentAuditor()
                .flatMap(authenticatedUserLoader::findById)
                .map(ShootingPolicy::fullName)
                .orElseThrow(() -> new UnauthorizedException("Une session active est requise."));
    }

    private static String fullName(AuthenticatedUser user) {
        return user.firstName() + " " + user.lastName();
    }

    private static @Nullable String note(@Nullable String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }
}
