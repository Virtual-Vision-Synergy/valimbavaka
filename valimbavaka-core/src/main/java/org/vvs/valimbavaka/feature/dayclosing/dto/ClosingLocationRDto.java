package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.util.List;
import java.util.UUID;

/** Reconciliation of one location (store, delivery, salon): the articles that held or moved stock today, by name. */
public record ClosingLocationRDto(UUID locationId, String code, String label, List<ClosingLineRDto> lines) {}
