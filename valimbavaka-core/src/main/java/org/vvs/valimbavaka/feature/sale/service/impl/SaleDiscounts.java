package org.vvs.valimbavaka.feature.sale.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionBasketLine;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.feature.sale.domain.PricedSaleLine;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleDiscount;

/**
 * Discount of a sale decided by the server (PRO-02, VTE-04): the best automatic promotion of the channel on the lines
 * sold at the catalogue price, or the promo code typed by the customer if it gives at least as much. A line with an
 * agreed price (an order delivered, LIV-03) is already negotiated: it is left out.
 */
@Component
class SaleDiscounts {

    private final PromotionService promotionService;
    private final ArticleService articleService;

    SaleDiscounts(PromotionService promotionService, ArticleService articleService) {
        this.promotionService = promotionService;
        this.articleService = articleService;
    }

    /** {@link SaleDiscount#NONE} when no promotion reaches the catalogue-priced lines of the sale. */
    SaleDiscount of(SaleCommand command, List<PricedSaleLine> lines) {
        var catalogued = new HashSet<UUID>();
        for (var line : command.lines()) {
            if (line.unitPrice() == null) {
                catalogued.add(line.articleId());
            }
        }
        var eligible = lines.stream()
                .filter(line -> catalogued.contains(line.articleId()))
                .toList();
        var promoCode = command.promoCode();
        if (eligible.isEmpty() && promoCode == null) {
            return SaleDiscount.NONE;
        }
        var ranges = rangesOf(eligible);
        var basket = eligible.stream()
                .map(line -> new PromotionBasketLine(
                        line.articleId(), ranges.get(line.articleId()), line.unitPrice(), line.quantity()))
                .toList();
        var discount = promoCode == null
                ? promotionService.priceBasket(command.salesChannelId(), basket)
                : promotionService.priceBasket(command.salesChannelId(), promoCode, basket);
        return new SaleDiscount(discount.promotionId(), discount.amount());
    }

    /** Article id → range id; an article without a range is absent. */
    private Map<UUID, UUID> rangesOf(List<PricedSaleLine> lines) {
        if (lines.isEmpty()) {
            return Map.of();
        }
        var ids = lines.stream().map(PricedSaleLine::articleId).toList();
        var ranges = new HashMap<UUID, UUID>();
        for (ArticleRDto article : articleService.getArticlesByIds(ids)) {
            if (article.rangeId() != null) {
                ranges.put(article.id(), article.rangeId());
            }
        }
        return ranges;
    }
}
