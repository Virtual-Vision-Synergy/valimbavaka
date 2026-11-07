package org.vvs.valimbavaka.feature.production.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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
}
