package org.vvs.valimbavaka.feature.referential.service;

import java.util.Optional;
import java.util.UUID;

import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Lecture des référentiels actifs ; interface publique pour les autres features (vente, stock). */
public interface ReferentialService {

    PageResponse<SalesChannelRDto> getSalesChannels(PageQuery query);

    PageResponse<ProductRangeRDto> getProductRanges(PageQuery query);

    PageResponse<StockLocationRDto> getStockLocations(PageQuery query);

    /** Vrai si le canal de vente existe et est actif (contrôle de référence pour les autres features). */
    boolean isActiveSalesChannel(UUID salesChannelId);

    /** Vrai si la gamme existe et est active (contrôle de référence pour les articles). */
    boolean isActiveProductRange(UUID productRangeId);

    /** Vrai si l'emplacement de stock existe et est actif (contrôle de référence pour les mouvements de stock). */
    boolean isActiveStockLocation(UUID stockLocationId);

    /** Identifiant du canal de vente de ce code, actif ou non (ex. {@code STORE} pour une prestation salon). */
    Optional<UUID> findSalesChannelId(String code);

    /** Identifiant de l'emplacement de stock de ce code, actif ou non (ex. {@code TRADE_SHOW}, le salon). */
    Optional<UUID> findStockLocationId(String code);

    /**
     * Identifiant de l'emplacement actif portant ce code stable (ex. {@code WAREHOUSE}), pour les features qui
     * mouvementent un emplacement fixe (production PRD-02).
     *
     * @throws org.vvs.valimbavaka.core.exception.BusinessException si l'emplacement est inconnu ou inactif (422)
     */
    UUID requireActiveStockLocationId(String code);
}
