package org.vvs.valimbavaka.feature.transfer.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One article moved by a transfer.
 *
 * @param quantity strictly positive, 3 decimals at most: it leaves the origin and enters the destination
 */
public record TransferLineCommand(UUID articleId, BigDecimal quantity) {}
