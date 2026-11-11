package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;

/**
 * Automatic promotion usable today, as the till needs it to show the discount before the sale (PRO-02). The server
 * recomputes the discount when the sale is recorded and decides.
 *
 * @param kind {@code PERCENT} (value %), {@code AMOUNT} (value ariary off the eligible lines) or {@code BUY_X} (one
 *     free unit per {@code value} bought)
 * @param articleIds sellable articles of the form that the promotion reaches (catalogue, range or articles)
 * @param channelIds sales channels on which it applies
 */
public record SalePromotionRDto(
        UUID id, String name, PromotionKind kind, BigDecimal value, List<UUID> articleIds, List<UUID> channelIds) {}
