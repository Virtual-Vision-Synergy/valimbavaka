package org.vvs.valimbavaka.feature.day.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/**
 * Active finished article available at the warehouse, with its minimum threshold (STK-04). {@code yesterday} holds
 * the quantities sent to each destination on the previous working day (JR-02); empty if none were sent.
 */
public record DayOpeningLineRDto(
        UUID articleId,
        String code,
        String name,
        ArticleUnit unit,
        BigDecimal threshold,
        BigDecimal warehouse,
        List<DayQuantityRDto> yesterday) {}
