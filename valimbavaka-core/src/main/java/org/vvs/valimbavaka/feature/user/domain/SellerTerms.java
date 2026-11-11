package org.vvs.valimbavaka.feature.user.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Conditions de vente d'une vendeuse : canal de vente (référence par identifiant), taux de commission (%) et objectif
 * mensuel de chiffre d'affaires en ariary (COM-04, fixé par l'administrateur seul ; facultatif).
 */
public record SellerTerms(
        @Nullable UUID salesChannelId, @Nullable BigDecimal commissionRate, @Nullable BigDecimal monthlyTarget) {

    public static final SellerTerms NONE = new SellerTerms(null, null, null);

    /** Conditions sans objectif mensuel. */
    public SellerTerms(@Nullable UUID salesChannelId, @Nullable BigDecimal commissionRate) {
        this(salesChannelId, commissionRate, null);
    }

    public boolean isEmpty() {
        return salesChannelId == null && commissionRate == null && monthlyTarget == null;
    }

    public boolean isComplete() {
        return salesChannelId != null && commissionRate != null;
    }
}
