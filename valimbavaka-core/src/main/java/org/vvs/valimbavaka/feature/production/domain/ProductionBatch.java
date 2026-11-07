package org.vvs.valimbavaka.feature.production.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Production batch: an output article obtained from consumed inputs (PRD-01). A batch is never deleted (JNL-02):
 * its status evolves (PRD-03) and its completion moves the stock (PRD-02).
 */
@Entity
@Table(name = "production_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductionBatch extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "output_article_id", nullable = false)
    private UUID outputArticleId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductionBatchStatus status;

    @Column(name = "completed_at")
    private @Nullable Instant completedAt;

    // Loaded lazily, by batches of pages, to keep the paged list free of N+1 queries.
    @ElementCollection
    @CollectionTable(name = "production_batch_inputs", joinColumns = @JoinColumn(name = "batch_id"))
    @OrderColumn(name = "line_no")
    @BatchSize(size = 100)
    private List<ProductionInput> inputs = new ArrayList<>();

    /** New batch, {@link ProductionBatchStatus#PLANNED}: nothing moves in the stock yet. */
    public static ProductionBatch plan(String number, ProductionBatchDetails details) {
        var batch = new ProductionBatch();
        batch.number = number;
        batch.outputArticleId = details.outputArticleId();
        batch.quantity = details.quantity();
        batch.operatorId = details.operatorId();
        batch.inputs = new ArrayList<>(details.inputs());
        batch.status = ProductionBatchStatus.PLANNED;
        return batch;
    }

    /**
     * Completion (PRD-02): freezes the unit cost of each input (absent from {@code costs} = no cost) and marks the
     * batch {@link ProductionBatchStatus#COMPLETED}. The stock movements are recorded by the service in the same
     * transaction.
     *
     * @throws org.vvs.valimbavaka.core.exception.ConflictException if the batch is already completed
     */
    public void complete(Map<UUID, BigDecimal> costs, Instant completedAt) {
        status.ensureCanComplete();
        for (var i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            inputs.set(i, input.withUnitCost(costs.get(input.getArticleId())));
        }
        this.status = ProductionBatchStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public List<ProductionInput> getInputs() {
        return List.copyOf(inputs);
    }
}
