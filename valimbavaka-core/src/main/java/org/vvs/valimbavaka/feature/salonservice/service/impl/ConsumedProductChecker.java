package org.vvs.valimbavaka.feature.salonservice.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.salonservice.domain.ConsumedProduct;

/**
 * Checks the products consumed by a salon service: an article is listed once, and a newly listed article must exist
 * and be active. An article already listed by the service is not checked again, so a service whose article was
 * deactivated since remains editable (same rule as the supplier of an article, ART-01).
 */
@Component
public class ConsumedProductChecker {

    private final ArticleService articleService;

    public ConsumedProductChecker(ArticleService articleService) {
        this.articleService = articleService;
    }

    void check(List<ConsumedProduct> products, Set<UUID> alreadyListed) {
        var seen = new HashSet<UUID>();
        for (ConsumedProduct product : products) {
            var articleId = product.getArticleId();
            if (!seen.add(articleId)) {
                throw new BusinessException("Un article ne peut être listé qu'une fois : " + articleId);
            }
            if (!alreadyListed.contains(articleId) && !articleService.isActiveArticle(articleId)) {
                throw new BusinessException("Article introuvable ou inactif : " + articleId);
            }
        }
    }
}
