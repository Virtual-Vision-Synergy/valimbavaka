package org.vvs.valimbavaka.feature.production.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ProductionUsage;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.production.dto.ProductionBatchWDto;
import org.vvs.valimbavaka.feature.production.dto.ProductionInputWDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

/** Business rules of a new batch; the server decides whatever the screen proposed (ART-04). */
@Component
class ProductionBatchPolicy {

    static final String DUPLICATE_INPUT = "Un entrant ne peut apparaître qu'une fois dans un lot.";
    static final String NOT_A_SELLER = "La personne responsable doit être une vendeuse active.";

    private final ArticleService articleService;
    private final UserService userService;

    ProductionBatchPolicy(ArticleService articleService, UserService userService) {
        this.articleService = articleService;
        this.userService = userService;
    }

    /** @return the output article, for the journal label */
    ArticleRDto check(ProductionBatchWDto request) {
        ensureDistinct(request.inputs());
        if (!userService.isActiveSeller(request.operatorId())) {
            throw new BusinessException(NOT_A_SELLER);
        }
        var output = articleService.requireProductionArticle(request.outputArticleId(), ProductionUsage.OUTPUT);
        request.inputs()
                .forEach(input -> articleService.requireProductionArticle(input.articleId(), ProductionUsage.INPUT));
        return output;
    }

    private static void ensureDistinct(List<ProductionInputWDto> inputs) {
        Set<UUID> seen = new HashSet<>();
        for (var input : inputs) {
            if (!seen.add(input.articleId())) {
                throw new BusinessException(DUPLICATE_INPUT);
            }
        }
    }
}
