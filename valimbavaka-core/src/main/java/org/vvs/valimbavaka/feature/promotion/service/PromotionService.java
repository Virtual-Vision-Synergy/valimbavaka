package org.vvs.valimbavaka.feature.promotion.service;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

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
     * Creates a promotion after checking its period, value, targets and channels (422) and the uniqueness of its
     * code (409); traced in the journal.
     */
    PromotionRDto createPromotion(PromotionWDto request);
}
