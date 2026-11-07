package org.vvs.valimbavaka.feature.transfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/** Line of a transfer sheet (TRF-04): the article with its current code, name and unit, and the quantity moved. */
public record TransferDetailLineRDto(
        UUID articleId, String articleCode, String articleName, ArticleUnit unit, BigDecimal quantity) {}
