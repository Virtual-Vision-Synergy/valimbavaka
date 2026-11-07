package org.vvs.valimbavaka.feature.production.domain;

/**
 * Life cycle of a production batch. PRD-01 only creates {@link #PLANNED} batches; the transitions belong to PRD-03
 * and the stock movements generated on completion to PRD-02.
 */
public enum ProductionBatchStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED
}
