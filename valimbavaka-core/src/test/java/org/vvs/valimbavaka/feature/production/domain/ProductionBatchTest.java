package org.vvs.valimbavaka.feature.production.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.core.exception.ConflictException;

class ProductionBatchTest {

    private static final UUID OUTPUT_ID = UUID.randomUUID();
    private static final UUID OPERATOR_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();

    @Test
    void plan_validDetails_createsAPlannedBatchWithItsInputs() {
        // Arrange
        var oil = new ProductionInput(OIL_ID, new BigDecimal("0.45"));
        var details = new ProductionBatchDetails(OUTPUT_ID, new BigDecimal("30"), OPERATOR_ID, List.of(oil));

        // Act
        var sut = ProductionBatch.plan("LOT-00001", details);

        // Assert
        assertThat(sut.getNumber()).isEqualTo("LOT-00001");
        assertThat(sut.getStatus()).isEqualTo(ProductionBatchStatus.PLANNED);
        assertThat(sut.getOutputArticleId()).isEqualTo(OUTPUT_ID);
        assertThat(sut.getQuantity()).isEqualByComparingTo("30");
        assertThat(sut.getOperatorId()).isEqualTo(OPERATOR_ID);
        assertThat(sut.getInputs()).singleElement().satisfies(input -> {
            assertThat(input.getArticleId()).isEqualTo(OIL_ID);
            assertThat(input.getQuantity()).isEqualByComparingTo("0.45");
        });
    }

    @Test
    void getInputs_listChangedByTheCaller_doesNotChangeTheBatch() {
        // Arrange
        var inputs = new ArrayList<>(List.of(new ProductionInput(OIL_ID, BigDecimal.ONE)));
        var details = new ProductionBatchDetails(OUTPUT_ID, BigDecimal.ONE, OPERATOR_ID, inputs);
        var sut = ProductionBatch.plan("LOT-00002", details);

        // Act
        inputs.clear();

        // Assert
        assertThat(sut.getInputs()).hasSize(1);
        assertThatThrownBy(() -> sut.getInputs().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void complete_plannedBatch_becomesCompletedWithFrozenCosts() {
        // Arrange
        var sut = plannedBatch();
        var at = Instant.parse("2026-11-08T10:00:00Z");

        // Act
        sut.complete(Map.of(OIL_ID, new BigDecimal("12000.00")), at);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(ProductionBatchStatus.COMPLETED);
        assertThat(sut.getCompletedAt()).isEqualTo(at);
        assertThat(sut.getInputs()).singleElement().satisfies(input -> {
            assertThat(input.getUnitCost()).isEqualByComparingTo("12000");
            assertThat(input.getQuantity()).isEqualByComparingTo("0.45");
        });
    }

    @Test
    void complete_inputWithoutCost_keepsANullUnitCost() {
        // Arrange
        var sut = plannedBatch();

        // Act
        sut.complete(Map.of(), Instant.parse("2026-11-08T10:00:00Z"));

        // Assert
        assertThat(sut.getInputs())
                .singleElement()
                .satisfies(input -> assertThat(input.getUnitCost()).isNull());
    }

    @Test
    void complete_alreadyCompletedBatch_throwsConflictAndKeepsTheFirstCompletion() {
        // Arrange
        var sut = plannedBatch();
        var first = Instant.parse("2026-11-08T10:00:00Z");
        sut.complete(Map.of(OIL_ID, BigDecimal.TEN), first);
        var second = Instant.parse("2026-11-09T10:00:00Z");

        // Act + Assert
        assertThatThrownBy(() -> sut.complete(Map.of(OIL_ID, BigDecimal.ONE), second))
                .isInstanceOf(ConflictException.class);
        assertThat(sut.getCompletedAt()).isEqualTo(first);
        assertThat(sut.getInputs().getFirst().getUnitCost()).isEqualByComparingTo("10");
    }

    @Test
    void start_plannedBatch_becomesInProgressWithoutCompletionDate() {
        // Arrange
        var sut = plannedBatch();

        // Act
        sut.start();

        // Assert
        assertThat(sut.getStatus()).isEqualTo(ProductionBatchStatus.IN_PROGRESS);
        assertThat(sut.getCompletedAt()).isNull();
    }

    @Test
    void start_inProgressBatch_throwsConflict() {
        // Arrange
        var sut = plannedBatch();
        sut.start();

        // Act + Assert
        assertThatThrownBy(sut::start)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà en cours");
        assertThat(sut.getStatus()).isEqualTo(ProductionBatchStatus.IN_PROGRESS);
    }

    @Test
    void start_completedBatch_throwsConflictAndStaysCompleted() {
        // Arrange
        var sut = plannedBatch();
        sut.complete(Map.of(), Instant.parse("2026-11-08T10:00:00Z"));

        // Act + Assert
        assertThatThrownBy(sut::start)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà terminé");
        assertThat(sut.getStatus()).isEqualTo(ProductionBatchStatus.COMPLETED);
    }

    @Test
    void complete_inProgressBatch_becomesCompleted() {
        // Arrange
        var sut = plannedBatch();
        sut.start();
        var at = Instant.parse("2026-11-08T16:00:00Z");

        // Act
        sut.complete(Map.of(OIL_ID, BigDecimal.TEN), at);

        // Assert
        assertThat(sut.getStatus()).isEqualTo(ProductionBatchStatus.COMPLETED);
        assertThat(sut.getCompletedAt()).isEqualTo(at);
    }

    private static ProductionBatch plannedBatch() {
        var oil = new ProductionInput(OIL_ID, new BigDecimal("0.45"));
        return ProductionBatch.plan(
                "LOT-00005", new ProductionBatchDetails(OUTPUT_ID, BigDecimal.ONE, OPERATOR_ID, List.of(oil)));
    }
}
