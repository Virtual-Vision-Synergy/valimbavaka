package org.vvs.valimbavaka.feature.day.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/** Active finished article available at the warehouse, with its minimum threshold (STK-04). */
public record DayOpeningLineRDto(
        UUID articleId, String code, String name, ArticleUnit unit, BigDecimal threshold, BigDecimal warehouse) {}
