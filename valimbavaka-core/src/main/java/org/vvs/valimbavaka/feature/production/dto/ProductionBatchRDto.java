package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;

/** Read DTO of a production batch; articles and seller are referenced by identifier. */
public record ProductionBatchRDto(
        UUID id,
        String number,
        UUID outputArticleId,
        BigDecimal quantity,
        UUID operatorId,
        ProductionBatchStatus status,
        List<ProductionInputRDto> inputs,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
