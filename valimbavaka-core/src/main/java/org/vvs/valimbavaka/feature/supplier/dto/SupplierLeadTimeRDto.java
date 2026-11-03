package org.vvs.valimbavaka.feature.supplier.dto;

import java.util.UUID;

import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;

/** Restocking lead time of a supplier, read by the threshold alerts (FRS-04). */
public record SupplierLeadTimeRDto(UUID id, String name, SupplierType type, int leadTimeDays) {}
