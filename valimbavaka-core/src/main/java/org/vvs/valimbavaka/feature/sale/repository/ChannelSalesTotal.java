package org.vvs.valimbavaka.feature.sale.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Turnover and number of validated sales of one sales channel over a period (VTE-08). */
public record ChannelSalesTotal(UUID salesChannelId, BigDecimal revenue, long salesCount) {}
