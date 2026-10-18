package org.vvs.valimbavaka.feature.user.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Conditions de vente d'une vendeuse : canal de vente (référence par identifiant) et taux de commission (%). */
public record SellerTerms(@Nullable UUID salesChannelId, @Nullable BigDecimal commissionRate) {

    public static final SellerTerms NONE = new SellerTerms(null, null);

    public boolean isEmpty() {
        return salesChannelId == null && commissionRate == null;
    }

    public boolean isComplete() {
        return salesChannelId != null && commissionRate != null;
    }
}
