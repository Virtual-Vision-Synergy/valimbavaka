package org.vvs.valimbavaka.feature.sale.service.impl;

import java.util.List;

import org.vvs.valimbavaka.feature.sale.domain.PricedSaleLine;
import org.vvs.valimbavaka.feature.sale.dto.SaleChannelOptionRDto;

/** A sale request that passed every rule: its channel (with the stock location) and its priced lines. */
record CheckedSale(SaleChannelOptionRDto channel, List<PricedSaleLine> lines) {}
