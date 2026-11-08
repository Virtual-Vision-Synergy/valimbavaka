package org.vvs.valimbavaka.feature.delivery.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Line of an order to deliver with the unit price of the article read at creation time. */
public record DeliveryOrderLineCommand(UUID articleId, BigDecimal quantity, BigDecimal unitPrice) {}
