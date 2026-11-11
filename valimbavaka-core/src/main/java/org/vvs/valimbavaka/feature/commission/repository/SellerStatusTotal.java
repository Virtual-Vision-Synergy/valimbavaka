package org.vvs.valimbavaka.feature.commission.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;

/** Count and total of the commissions of a seller in one status (projection of {@link CommissionRepository}). */
public interface SellerStatusTotal {

    UUID getSellerId();

    CommissionStatus getStatus();

    long getCommissionCount();

    BigDecimal getTotalAmount();
}
