package org.vvs.valimbavaka.feature.transfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferLineRDto(UUID articleId, BigDecimal quantity) {}
