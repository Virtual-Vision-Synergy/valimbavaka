package org.vvs.valimbavaka.feature.promotion.service;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.promotion.domain.PromotionBasketLine;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDiscount;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionStatus;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionWDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Promotion referential (PRO-01); the till (PRO-02, VTE-04) reads the promotions through this interface. */
public interface PromotionService {

    /**
     * Paged list (PRO-04), searchable on name and code; {@code status} null = every status. Sort white list:
     * {@code startsOn} (default, descending), {@code endsOn}, {@code name}, {@code createdAt}.
     */
    PageResponse<PromotionRDto> getPromotions(PageQuery query, @Nullable PromotionStatus status);

    PromotionRDto getPromotionById(UUID promotionId);

    /**
     * Promotions active today (Antananarivo) that reach this article (ART-06): whole catalogue, its range
     * ({@code rangeId}, null if none) or the article itself; closest end first. Usage limit not considered.
     */
    List<PromotionRDto> getActivePromotionsFor(UUID articleId, @Nullable UUID rangeId);

    /**
     * Automatic promotions (no code) usable today (Antananarivo) on at least one channel: active period and usage
     * limit not reached; closest end first (PRO-02). Used by the sale form so the till can show the discount.
     */
    List<PromotionRDto> getAutomaticPromotions();

    /**
     * Discount of the best automatic promotion for a basket sold on a channel today (PRO-02): among the automatic
     * promotions active on that channel, the one that gives the customer the largest discount (the earliest end,
     * then the name, on a tie); no accumulation. {@link PromotionDiscount#NONE} if the basket is empty or no
     * promotion reaches it. The server decides the discount of a sale with this method.
     */
    PromotionDiscount priceBasket(UUID salesChannelId, List<PromotionBasketLine> lines);

    /**
     * Creates a promotion after checking its period, value, targets and channels (422) and the uniqueness of its
     * code (409); traced in the journal.
     */
    PromotionRDto createPromotion(PromotionWDto request);

    /**
     * Counts one use of the promotion for a sale that carries it (PRO-03), in the caller's transaction. The check of
     * the usage limit and the increment are one atomic statement: two simultaneous sales cannot go past the limit.
     * Throws {@code ConflictException} (409) if the limit was reached in the meantime, {@code NotFoundException} if
     * the promotion does not exist.
     */
    void recordUsage(UUID promotionId);

    /**
     * Gives back the use counted by {@link #recordUsage} when the sale is cancelled (PRO-03); the counter never goes
     * below zero. No effect on an unknown promotion.
     */
    void releaseUsage(UUID promotionId);
}
