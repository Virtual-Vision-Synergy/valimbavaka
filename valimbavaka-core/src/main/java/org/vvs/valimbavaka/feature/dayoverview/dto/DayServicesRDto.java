package org.vvs.valimbavaka.feature.dayoverview.dto;

import java.math.BigDecimal;

/** Salon services performed today: number and revenue (sum of the prices charged). */
public record DayServicesRDto(long count, BigDecimal revenue) {}
