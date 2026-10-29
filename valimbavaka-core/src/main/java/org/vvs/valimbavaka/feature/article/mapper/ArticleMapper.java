package org.vvs.valimbavaka.feature.article.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import org.vvs.valimbavaka.feature.article.domain.Article;
import org.vvs.valimbavaka.feature.article.dto.ArticleExportRDto;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;

/** Read-only: creation and update go through the business methods of {@link Article}. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ArticleMapper {

    ArticleRDto toRDto(Article article);

    ArticleExportRDto toExportRDto(Article article);
}
