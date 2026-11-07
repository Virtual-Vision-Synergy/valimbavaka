package org.vvs.valimbavaka.feature.production.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchDetails;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;

@ExtendWith(MockitoExtension.class)
class ProductionCompletionTest {

    private static final Instant NOW = Instant.parse("2026-11-08T09:30:00Z");
    private static final UUID WAREHOUSE_ID = UUID.randomUUID();
    private static final UUID OUTPUT_ID = UUID.randomUUID();
    // Input identifiers chosen so that the lock order (sorted by UUID) differs from the input order.
    private static final UUID OIL_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID BASE_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private ArticleService articleService;

    @Mock
    private ReferentialService referentialService;

    private ProductionCompletion sut;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        sut = new ProductionCompletion(stockMovementService, articleService, referentialService, clock);
    }

    @Test
    void complete_plannedBatch_recordsOneOutgoingMovementPerInputThenTheIncomingOutput() {
        // Arrange
        var batch = batch();
        when(articleService.getCosts(List.of(OIL_ID, BASE_ID))).thenReturn(Map.of(OIL_ID, new BigDecimal("12000")));
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE_ID);

        // Act
        sut.complete(batch);

        // Assert
        var commands = ArgumentCaptor.forClass(StockMovementCommand.class);
        verify(stockMovementService, times(3)).record(commands.capture());
        assertThat(commands.getAllValues())
                .extracting(StockMovementCommand::articleId)
                .containsExactly(BASE_ID, OIL_ID, OUTPUT_ID);
        assertThat(commands.getAllValues())
                .extracting(StockMovementCommand::quantity)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("-1.5"), new BigDecimal("-0.45"), new BigDecimal("30"));
        assertThat(commands.getAllValues()).allSatisfy(command -> {
            assertThat(command.type()).isEqualTo(StockMovementType.PRODUCTION);
            assertThat(command.locationId()).isEqualTo(WAREHOUSE_ID);
            assertThat(command.referenceId()).isEqualTo(batch.getId());
        });
    }

    @Test
    void complete_plannedBatch_freezesTheCostsAndMarksTheBatchCompleted() {
        // Arrange
        var batch = batch();
        when(articleService.getCosts(List.of(OIL_ID, BASE_ID))).thenReturn(Map.of(OIL_ID, new BigDecimal("12000")));
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE_ID);

        // Act
        sut.complete(batch);

        // Assert
        assertThat(batch.getStatus()).isEqualTo(ProductionBatchStatus.COMPLETED);
        assertThat(batch.getCompletedAt()).isEqualTo(NOW);
        assertThat(batch.getInputs())
                .extracting(ProductionInput::getUnitCost)
                .containsExactly(new BigDecimal("12000"), null);
    }

    @Test
    void complete_unknownWarehouse_recordsNoMovement() {
        // Arrange
        var batch = batch();
        when(articleService.getCosts(any())).thenReturn(Map.of());
        when(referentialService.requireActiveStockLocationId("WAREHOUSE"))
                .thenThrow(new BusinessException("Emplacement de stock WAREHOUSE inconnu ou inactif."));

        // Act + Assert
        assertThatThrownBy(() -> sut.complete(batch)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(stockMovementService);
    }

    @Test
    void complete_insufficientStock_propagatesTheConflictWithoutRecordingTheOutput() {
        // Arrange
        var batch = batch();
        when(articleService.getCosts(any())).thenReturn(Map.of());
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE_ID);
        when(stockMovementService.record(any())).thenThrow(new ConflictException("Stock insuffisant"));

        // Act + Assert
        assertThatThrownBy(() -> sut.complete(batch)).isInstanceOf(ConflictException.class);
        verify(stockMovementService, times(1)).record(any());
    }

    private static ProductionBatch batch() {
        var inputs = List.of(
                new ProductionInput(OIL_ID, new BigDecimal("0.45")),
                new ProductionInput(BASE_ID, new BigDecimal("1.5")));
        var details = new ProductionBatchDetails(OUTPUT_ID, new BigDecimal("30"), UUID.randomUUID(), inputs);
        return ProductionBatch.plan("LOT-00004", details);
    }
}
