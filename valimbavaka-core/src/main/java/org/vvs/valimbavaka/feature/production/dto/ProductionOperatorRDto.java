package org.vvs.valimbavaka.feature.production.dto;

import java.util.UUID;

/** Active seller who can be responsible for a batch; no email nor commission rate is exposed. */
public record ProductionOperatorRDto(UUID id, String firstName, String lastName) {}
