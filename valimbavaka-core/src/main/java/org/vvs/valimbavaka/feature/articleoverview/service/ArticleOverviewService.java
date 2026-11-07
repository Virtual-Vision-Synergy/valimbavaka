package org.vvs.valimbavaka.feature.articleoverview.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.articleoverview.dto.ArticleOverviewRDto;

/** Article sheet at 360° (ART-06): read only, assembled from the article, stock, promotion and production. */
public interface ArticleOverviewService {

    /**
     * Sheet of an active or deactivated article; 404 if the article is unknown.
     *
     * @throws org.vvs.valimbavaka.core.exception.NotFoundException if the article is unknown
     */
    ArticleOverviewRDto getOverview(UUID articleId);
}
