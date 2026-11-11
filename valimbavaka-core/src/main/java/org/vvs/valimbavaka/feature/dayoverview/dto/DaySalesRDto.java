package org.vvs.valimbavaka.feature.dayoverview.dto;

import java.math.BigDecimal;
import java.util.List;

import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;

/** Validated sales of the day (cancelled and reversed sales are not counted): turnover, count and split per channel. */
public record DaySalesRDto(BigDecimal revenue, long count, List<ChannelSalesRDto> channels) {}
