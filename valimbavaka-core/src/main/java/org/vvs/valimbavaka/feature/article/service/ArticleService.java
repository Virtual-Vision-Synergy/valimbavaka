package org.vvs.valimbavaka.feature.article.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.domain.ProductionUsage;
import org.vvs.valimbavaka.feature.article.dto.ArticleExportRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

public interface ArticleService {

    PageResponse<ArticleRDto> getArticles(PageQuery query, ArticleFilter filter);

    /**
     * Rows of the article export (ART-08): same filters and search as the list, sorted by code by default, without
     * the purchase cost.
     */
    PageResponse<ArticleExportRDto> getExportableArticles(PageQuery query, ArticleFilter filter);

    ArticleRDto getArticleById(UUID articleId);

    /** Purchase cost of the given articles (STK-03); an unknown article or one without cost is absent. */
    Map<UUID, BigDecimal> getCosts(Collection<UUID> articleIds);

    /** Active articles with a minimum threshold above zero, by code (STK-04: threshold alerts). */
    List<ArticleRDto> getArticlesWithThreshold();

    /** Active articles usable in production with this usage (ART-04): MP/SF as inputs, finished products as outputs. */
    PageResponse<ArticleRDto> getProductionArticles(PageQuery query, ProductionUsage usage);

    /**
     * Server-side check for the production batches (PRD-01): 404 if the article is unknown, 422 if it is inactive
     * or if its type does not allow this usage.
     */
    ArticleRDto requireProductionArticle(UUID articleId, ProductionUsage usage);

    ArticleRDto createArticle(ArticleWDto request);

    ArticleRDto updateArticle(UUID articleId, ArticleWDto request);

    /** Activates or deactivates an article (idempotent: no trace if the state is already the requested one). */
    ArticleRDto setActive(UUID articleId, boolean active);
}
