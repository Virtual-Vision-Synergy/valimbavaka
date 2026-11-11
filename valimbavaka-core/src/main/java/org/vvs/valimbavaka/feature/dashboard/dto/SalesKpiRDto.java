package org.vvs.valimbavaka.feature.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;

/**
 * Validated sales of today: number, number the day before, average basket and the split per channel (VTE-08, every
 * active channel, best turnover first).
 */
public record SalesKpiRDto(long count, long previousCount, BigDecimal averageBasket, List<ChannelSalesRDto> channels) {}
