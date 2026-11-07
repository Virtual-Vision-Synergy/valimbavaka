package org.vvs.valimbavaka.feature.production.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.BASE_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OIL_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OPERATOR_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OUTPUT_ID;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchDetails;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.production.repository.ProductionBatchRepository;

@ExtendWith(MockitoExtension.class)
class ProductionIndicatorServiceImplTest {

    /** 8 November 2026, 11:00 in Madagascar. */
    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");

    private static final Map<UUID, BigDecimal> COSTS =
            Map.of(OIL_ID, new BigDecimal("1200.00"), BASE_ID, new BigDecimal("12000.00"));

    @Mock
    private ProductionBatchRepository productionBatchRepository;

    private ProductionIndicatorServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ProductionIndicatorServiceImpl(productionBatchRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getIndicators_completedBatchesOfTheMonth_countsBatchesUnitsAndMaterialCostPerUnit() {
        // Arrange: 30 soaps for 2 kg of oil + 0.5 kg of base (8 400 Ar), 10 soaps for 1 kg of oil (1 200 Ar)
        var first = completed("30", COSTS, input(OIL_ID, "2"), input(BASE_ID, "0.5"));
        var second = completed("10", COSTS, input(OIL_ID, "1"));
        arrange(List.of(first, second), "32", 3);

        // Act
        var result = sut.getIndicators();

        // Assert
        assertThat(result.month()).isEqualTo("2026-11");
        assertThat(result.completedBatches()).isEqualTo(2);
        assertThat(result.inProgressBatches()).isEqualTo(3);
        assertThat(result.unitsProduced()).isEqualByComparingTo("40");
        assertThat(result.previousMonthUnits()).isEqualByComparingTo("32");
        assertThat(result.unitsTrendPercent()).isEqualTo(25);
        assertThat(result.materialCost()).isEqualByComparingTo("9600");
        assertThat(result.materialCostPerUnit()).isEqualByComparingTo("240.00");
        assertThat(result.uncostedBatches()).isZero();
    }

    @Test
    void getIndicators_batchWithAnInputWithoutCost_leavesItOutOfTheCostAndCountsItAsUncosted() {
        // Arrange: the base has no frozen cost in the second batch
        var costed = completed("30", COSTS, input(OIL_ID, "2"), input(BASE_ID, "0.5"));
        var oilOnly = Map.of(OIL_ID, new BigDecimal("1200.00"));
        var uncosted = completed("10", oilOnly, input(OIL_ID, "1"), input(BASE_ID, "1"));
        arrange(List.of(costed, uncosted), "0", 0);

        // Act
        var result = sut.getIndicators();

        // Assert
        assertThat(result.unitsProduced()).isEqualByComparingTo("40");
        assertThat(result.materialCost()).isEqualByComparingTo("8400");
        assertThat(result.materialCostPerUnit()).isEqualByComparingTo("280.00");
        assertThat(result.uncostedBatches()).isEqualTo(1);
    }

    @Test
    void getIndicators_noBatchThisMonthNorLastMonth_returnsZerosAndNoAverageNorTrend() {
        // Arrange
        arrange(List.of(), "0", 0);

        // Act
        var result = sut.getIndicators();

        // Assert
        assertThat(result.completedBatches()).isZero();
        assertThat(result.unitsProduced()).isEqualByComparingTo("0");
        assertThat(result.materialCost()).isEqualByComparingTo("0");
        assertThat(result.materialCostPerUnit()).isNull();
        assertThat(result.unitsTrendPercent()).isNull();
    }

    @Test
    void getIndicators_fewerUnitsThanLastMonth_returnsANegativeTrend() {
        // Arrange: 30 units against 40 last month
        arrange(List.of(completed("30", COSTS, input(OIL_ID, "2"))), "40", 0);

        // Act
        var result = sut.getIndicators();

        // Assert
        assertThat(result.unitsTrendPercent()).isEqualTo(-25);
    }

    @Test
    void getIndicators_firstMinutesOfTheMonthInMadagascar_readsTheMonthOfMadagascar() {
        // Arrange: 30 November 2026 21:30 UTC is already 1 December 00:30 in Madagascar (UTC+3)
        var clock = Clock.fixed(Instant.parse("2026-11-30T21:30:00Z"), ZoneOffset.UTC);
        sut = new ProductionIndicatorServiceImpl(productionBatchRepository, clock);
        arrange(List.of(), "0", 0);
        var octoberStart = Instant.parse("2026-10-31T21:00:00Z");
        var novemberStart = Instant.parse("2026-11-30T21:00:00Z");
        var decemberStart = Instant.parse("2026-12-31T21:00:00Z");

        // Act
        var result = sut.getIndicators();

        // Assert
        assertThat(result.month()).isEqualTo("2026-12");
        verify(productionBatchRepository).findCompletedBetween(novemberStart, decemberStart);
        verify(productionBatchRepository).sumCompletedQuantityBetween(octoberStart, novemberStart);
    }

    private void arrange(List<ProductionBatch> batches, String previousUnits, long inProgress) {
        var previous = new BigDecimal(previousUnits);
        when(productionBatchRepository.findCompletedBetween(any(), any())).thenReturn(batches);
        when(productionBatchRepository.sumCompletedQuantityBetween(any(), any()))
                .thenReturn(previous);
        when(productionBatchRepository.countByStatus(ProductionBatchStatus.IN_PROGRESS))
                .thenReturn(inProgress);
    }

    private static ProductionBatch completed(String quantity, Map<UUID, BigDecimal> costs, ProductionInput... inputs) {
        var details = new ProductionBatchDetails(OUTPUT_ID, new BigDecimal(quantity), OPERATOR_ID, List.of(inputs));
        var batch = ProductionBatch.plan("LOT-00001", details);
        batch.complete(costs, NOW);
        return batch;
    }

    private static ProductionInput input(UUID articleId, String quantity) {
        return new ProductionInput(articleId, new BigDecimal(quantity));
    }
}
