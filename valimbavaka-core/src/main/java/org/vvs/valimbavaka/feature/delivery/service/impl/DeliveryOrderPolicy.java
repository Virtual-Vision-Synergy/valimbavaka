package org.vvs.valimbavaka.feature.delivery.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryCustomer;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderCommand;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderLineCommand;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderLineWDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderWDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

/**
 * Rules of an order to deliver checked before anything is written: an active driver, a day that is not past (in
 * Antananarivo), each article once, known, active and finished; the unit price is read from the article.
 */
@Component
class DeliveryOrderPolicy {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private final UserService userService;
    private final ArticleService articleService;
    private final Clock clock;

    DeliveryOrderPolicy(UserService userService, ArticleService articleService, Clock clock) {
        this.userService = userService;
        this.articleService = articleService;
        this.clock = clock;
    }

    DeliveryOrderCommand check(DeliveryOrderWDto request) {
        if (!userService.isActiveDriver(request.driverId())) {
            throw new BusinessException("Le livreur choisi est inconnu, désactivé ou n'a pas le rôle Livreur.");
        }
        if (request.scheduledDate().isBefore(LocalDate.now(clock.withZone(BUSINESS_ZONE)))) {
            throw new BusinessException("La date de livraison ne peut pas être dans le passé.");
        }
        var customer = new DeliveryCustomer(
                request.customerName().strip(),
                request.customerPhone().strip(),
                request.address().strip(),
                blankToNull(request.district()));
        return new DeliveryOrderCommand(
                customer, request.driverId(), request.scheduledDate(), blankToNull(request.note()), price(request));
    }

    private List<DeliveryOrderLineCommand> price(DeliveryOrderWDto request) {
        var articleIds = new HashSet<UUID>();
        var lines = new ArrayList<DeliveryOrderLineCommand>();
        for (DeliveryOrderLineWDto line : request.lines()) {
            if (!articleIds.add(line.articleId())) {
                throw new BusinessException("Un article ne peut figurer qu'une fois dans une commande.");
            }
            lines.add(new DeliveryOrderLineCommand(line.articleId(), line.quantity(), unitPrice(line.articleId())));
        }
        return lines;
    }

    private BigDecimal unitPrice(UUID articleId) {
        var article = articleService.getArticleById(articleId);
        var price = article.price();
        if (article.type() != ArticleType.FINISHED || !article.active() || price == null) {
            throw new BusinessException("L'article %s n'est pas un produit fini actif : il ne peut pas être livré."
                    .formatted(article.code()));
        }
        return price;
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
