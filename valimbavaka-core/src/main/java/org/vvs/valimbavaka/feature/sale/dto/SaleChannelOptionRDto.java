package org.vvs.valimbavaka.feature.sale.dto;

import java.util.UUID;

/** Active sales channel offered by the sale form, with the stock location its sales are taken from. */
public record SaleChannelOptionRDto(UUID id, String code, String label, UUID stockLocationId) {}
