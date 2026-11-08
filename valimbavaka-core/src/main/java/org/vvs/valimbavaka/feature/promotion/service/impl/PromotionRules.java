package org.vvs.valimbavaka.feature.promotion.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;

/**
 * Business rules of a promotion (422): coherent period not already over, value allowed by the kind, targets
 * consistent with the scope and active, active sales channels.
 */
@Component
public class PromotionRules {

    /** Business day of the shop: dates of a promotion are read in Madagascar time. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private static final BigDecimal MAX_PERCENT = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_BUY_X = BigDecimal.valueOf(2);
    private static final BigDecimal MAX_BUY_X = BigDecimal.valueOf(100);

    private final ReferentialService referentialService;
    private final ArticleService articleService;

    public PromotionRules(ReferentialService referentialService, ArticleService articleService) {
        this.referentialService = referentialService;
        this.articleService = articleService;
    }

    void check(PromotionDetails details, LocalDate today) {
        checkPeriod(details.period(), today);
        checkValue(details.kind(), details.value());
        checkChannels(details.channelIds());
        checkTargets(details.scope(), details.targetIds());
    }

    private static void checkPeriod(PromotionPeriod period, LocalDate today) {
        if (period.isReversed()) {
            throw new BusinessException("La date de fin doit suivre la date de début.");
        }
        if (period.endsOn().isBefore(today)) {
            throw new BusinessException("La promotion serait déjà terminée : la fin est au plus tôt aujourd'hui.");
        }
    }

    private static void checkValue(PromotionKind kind, BigDecimal value) {
        if (kind == PromotionKind.PERCENT && value.compareTo(MAX_PERCENT) > 0) {
            throw new BusinessException("Un pourcentage de remise ne dépasse pas 100.");
        }
        if (kind == PromotionKind.BUY_X && !isAllowedBuyX(value)) {
            throw new BusinessException("Pour « X achetés », indiquez un nombre entier d'articles entre 2 et 100.");
        }
    }

    private void checkChannels(Set<UUID> channelIds) {
        for (UUID channelId : channelIds) {
            if (!referentialService.isActiveSalesChannel(channelId)) {
                throw new BusinessException("Canal de vente introuvable ou désactivé : " + channelId);
            }
        }
    }

    private void checkTargets(PromotionScope scope, Set<UUID> targetIds) {
        if (scope == PromotionScope.ALL) {
            if (!targetIds.isEmpty()) {
                throw new BusinessException("Une promotion sur tout le catalogue ne cible ni gamme ni article.");
            }
            return;
        }
        if (targetIds.isEmpty()) {
            throw new BusinessException("Choisissez au moins une gamme ou un article ciblé.");
        }
        if (scope == PromotionScope.RANGES) {
            checkRanges(targetIds);
        } else {
            checkArticles(targetIds);
        }
    }

    private void checkRanges(Set<UUID> rangeIds) {
        for (UUID rangeId : rangeIds) {
            if (!referentialService.isActiveProductRange(rangeId)) {
                throw new BusinessException("Gamme introuvable ou désactivée : " + rangeId);
            }
        }
    }

    /** Only active finished products are sold, hence discounted. */
    private void checkArticles(Set<UUID> articleIds) {
        Set<UUID> sellable = articleService.getArticlesByIds(articleIds).stream()
                .filter(article -> article.active() && article.type() == ArticleType.FINISHED)
                .map(ArticleRDto::id)
                .collect(Collectors.toSet());
        for (UUID articleId : articleIds) {
            if (!sellable.contains(articleId)) {
                throw new BusinessException("Article introuvable, désactivé ou non vendu : " + articleId);
            }
        }
    }

    private static boolean isAllowedBuyX(BigDecimal value) {
        boolean whole = value.stripTrailingZeros().scale() <= 0;
        return whole && value.compareTo(MIN_BUY_X) >= 0 && value.compareTo(MAX_BUY_X) <= 0;
    }
}
