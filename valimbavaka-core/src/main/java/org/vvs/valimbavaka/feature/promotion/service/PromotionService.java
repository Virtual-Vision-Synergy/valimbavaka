package org.vvs.valimbavaka.feature.promotion.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionWDto;

/** Promotion referential (PRO-01); the till (PRO-02, VTE-04) reads the promotions through this interface. */
public interface PromotionService {

    PromotionRDto getPromotionById(UUID promotionId);

    /**
     * Creates a promotion after checking its period, value, targets and channels (422) and the uniqueness of its
     * code (409); traced in the journal.
     */
    PromotionRDto createPromotion(PromotionWDto request);
}
