package org.vvs.valimbavaka.feature.article.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Purchase cost of an article (STK-03: valuation of the stock). */
public record ArticleCost(UUID articleId, BigDecimal cost) {}
