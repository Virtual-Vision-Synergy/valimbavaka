package org.vvs.valimbavaka.feature.commission.repository;

import java.util.UUID;

/** A seller who has commissions over a period (projection of {@link CommissionRepository#searchSellers}). */
public interface SellerKey {

    UUID getSellerId();

    String getSellerName();
}
