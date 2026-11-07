package org.vvs.valimbavaka.feature.shooting.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Quantity of an article still out for shootings, all exits together (SHO-03: value tied up). */
public record ShootingArticleQuantity(UUID articleId, BigDecimal quantity) {}
