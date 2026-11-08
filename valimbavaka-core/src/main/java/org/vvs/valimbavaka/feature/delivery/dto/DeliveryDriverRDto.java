package org.vvs.valimbavaka.feature.delivery.dto;

import java.util.UUID;

/** Active driver who can be assigned to an order; no email nor any other account data is exposed. */
public record DeliveryDriverRDto(UUID id, String firstName, String lastName) {}
