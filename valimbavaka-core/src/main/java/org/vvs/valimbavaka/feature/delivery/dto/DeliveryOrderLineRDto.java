package org.vvs.valimbavaka.feature.delivery.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Line of an order: unit price copied at creation, {@code amount} = quantity × unit price (2 decimals). */
public record DeliveryOrderLineRDto(UUID articleId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {}
