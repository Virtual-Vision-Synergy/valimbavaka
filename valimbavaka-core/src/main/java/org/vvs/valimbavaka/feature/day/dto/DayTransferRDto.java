package org.vvs.valimbavaka.feature.day.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Morning transfer of a destination: its number, line count and total quantity. */
public record DayTransferRDto(
        UUID transferId, String transferNumber, UUID locationId, int lineCount, BigDecimal quantity) {}
