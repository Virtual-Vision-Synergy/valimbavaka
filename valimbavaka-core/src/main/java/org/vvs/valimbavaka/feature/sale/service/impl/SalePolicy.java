package org.vvs.valimbavaka.feature.sale.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.sale.domain.PricedSaleLine;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleLineCommand;
import org.vvs.valimbavaka.feature.user.service.UserService;

/**
 * Rules of a sale checked before anything is written: active channel, active seller (if any), 1 to 100 lines, each
 * article once, positive quantities, active finished articles with a price. A line with an agreed price (an order
 * delivered, LIV-03) only needs a finished article: the order was accepted when the article was still sold. The stock
 * balance is checked by the movements.
 */
@Component
class SalePolicy {

    static final int MAX_LINES = 100;

    private final SaleChannels channels;
    private final UserService userService;
    private final ArticleService articleService;

    SalePolicy(SaleChannels channels, UserService userService, ArticleService articleService) {
        this.channels = channels;
        this.userService = userService;
        this.articleService = articleService;
    }

    CheckedSale check(SaleCommand command) {
        var channel = channels.require(command.salesChannelId());
        var sellerId = command.sellerId();
        if (sellerId != null && !userService.isActiveSeller(sellerId)) {
            throw new BusinessException("La vendeuse est inconnue, inactive ou n'a pas le rôle vendeuse.");
        }
        return new CheckedSale(channel, price(command.lines()));
    }

    private List<PricedSaleLine> price(List<SaleLineCommand> lines) {
        if (lines.isEmpty() || lines.size() > MAX_LINES) {
            throw new BusinessException("Une vente compte de 1 à %d lignes.".formatted(MAX_LINES));
        }
        var articleIds = new HashSet<UUID>();
        var priced = new ArrayList<PricedSaleLine>();
        for (var line : lines) {
            if (!articleIds.add(line.articleId())) {
                throw new BusinessException("Un article ne peut figurer qu'une fois dans une vente.");
            }
            if (line.quantity().signum() <= 0) {
                throw new BusinessException("La quantité d'une ligne doit être supérieure à zéro.");
            }
            priced.add(priceOf(line, articleService.getArticleById(line.articleId())));
        }
        return priced;
    }

    /** The agreed price of the command if any, otherwise the current sale price of the article. */
    private static PricedSaleLine priceOf(SaleLineCommand line, ArticleRDto article) {
        var agreed = line.unitPrice();
        var unitPrice = agreed == null ? article.price() : agreed;
        var sellable = agreed != null || article.active();
        if (article.type() != ArticleType.FINISHED || !sellable || unitPrice == null) {
            var message = "L'article %s · %s n'est pas vendable : produit fini actif avec un prix requis.";
            throw new BusinessException(message.formatted(article.code(), article.name()));
        }
        if (unitPrice.signum() < 0 || unitPrice.stripTrailingZeros().scale() > 2) {
            throw new BusinessException("Le prix unitaire doit être positif, avec 2 décimales au maximum.");
        }
        return new PricedSaleLine(article.id(), line.quantity(), unitPrice, article.cost());
    }
}
