package org.vvs.valimbavaka.feature.article.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleFilter;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

public interface ArticleService {

    PageResponse<ArticleRDto> getArticles(PageQuery query, ArticleFilter filter);

    ArticleRDto getArticleById(UUID articleId);

    ArticleRDto createArticle(ArticleWDto request);

    ArticleRDto updateArticle(UUID articleId, ArticleWDto request);

    /** Activates or deactivates an article (idempotent: no trace if the state is already the requested one). */
    ArticleRDto setActive(UUID articleId, boolean active);
}
