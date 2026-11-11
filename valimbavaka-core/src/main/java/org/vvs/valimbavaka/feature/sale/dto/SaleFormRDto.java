package org.vvs.valimbavaka.feature.sale.dto;

import java.util.List;

/**
 * Everything the sale form needs in one call: channels (with their stock location), active sellers, sellable
 * articles with their stock and the automatic promotions usable today (PRO-02); {@code nextNumber} is the probable
 * number of the next sale, the real one being given when the sale is recorded.
 */
public record SaleFormRDto(
        long nextNumber,
        List<SaleChannelOptionRDto> channels,
        List<SaleSellerRDto> sellers,
        List<SaleArticleRDto> articles,
        List<SalePromotionRDto> promotions) {}
