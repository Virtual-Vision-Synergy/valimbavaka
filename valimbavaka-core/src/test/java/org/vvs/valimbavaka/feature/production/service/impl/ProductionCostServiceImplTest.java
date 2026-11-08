package org.vvs.valimbavaka.feature.production.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.BASE_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OIL_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OPERATOR_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OUTPUT_ID;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchDetails;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.production.dto.ProductionInputCostRDto;
import org.vvs.valimbavaka.feature.production.repository.ProductionBatchRepository;

@ExtendWith(MockitoExtension.class)
class ProductionCostServiceImplTest {

    private static final UUID BATCH_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");

    /** Costs frozen at completion: oil 1 200 Ar/kg, base 12 000 Ar/kg. */
    private static final Map<UUID, BigDecimal> FROZEN =
            Map.of(OIL_ID, new BigDecimal("1200.00"), BASE_ID, new BigDecimal("12000.00"));

    @Mock
    private ProductionBatchRepository productionBatchRepository;

    @Mock
    private ArticleService articleService;

    private ProductionCostServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ProductionCostServiceImpl(productionBatchRepository, articleService);
    }

    @Test
    void getBatchCost_completedBatch_usesTheFrozenCostsAndComputesTheCostPerUnit() {
        // Arrange: 30 soaps for 2 kg of oil (2 400 Ar) + 0.5 kg of base (6 000 Ar); the oil now costs 1 500 Ar
        var batch = planned("30", input(OIL_ID, "2"), input(BASE_ID, "0.5"));
        batch.complete(FROZEN, NOW);
        arrange(batch, "8500", "1500.00", "12000.00");

        // Act
        var result = sut.getBatchCost(BATCH_ID);

        // Assert
        assertThat(result.estimated()).isFalse();
        assertThat(result.completedAt()).isEqualTo(NOW);
        assertThat(result.inputs())
                .extracting(ProductionInputCostRDto::articleName, ProductionInputCostRDto::lineCost)
                .containsExactly(
                        tuple("Huile de coco", new BigDecimal("2400.00")),
                        tuple("Base savon", new BigDecimal("6000.00")));
        assertThat(result.inputs().getFirst().unitCost()).isEqualByComparingTo("1200");
        assertThat(result.materialCost()).isEqualByComparingTo("8400");
        assertThat(result.unitCost()).isEqualByComparingTo("280.00");
        assertThat(result.uncostedInputs()).isZero();
        assertThat(result.outputArticleName()).isEqualTo("Savon noir 100 g");
        assertThat(result.marginPercent()).isEqualTo(97);
    }

    @Test
    void getBatchCost_plannedBatch_estimatesWithTheCurrentPurchaseCosts() {
        // Arrange: 10 soaps for 1 kg of oil at 1 500 Ar now
        arrange(planned("10", input(OIL_ID, "1")), "8500", "1500.00", "12000.00");

        // Act
        var result = sut.getBatchCost(BATCH_ID);

        // Assert
        assertThat(result.estimated()).isTrue();
        assertThat(result.completedAt()).isNull();
        assertThat(result.materialCost()).isEqualByComparingTo("1500");
        assertThat(result.unitCost()).isEqualByComparingTo("150.00");
    }

    @Test
    void getBatchCost_inputWithoutCost_leavesTheTotalsNullAndCountsTheInput() {
        // Arrange: the base had no cost at completion
        var batch = planned("30", input(OIL_ID, "2"), input(BASE_ID, "0.5"));
        batch.complete(Map.of(OIL_ID, new BigDecimal("1200.00")), NOW);
        arrange(batch, "8500", "1200.00", null);

        // Act
        var result = sut.getBatchCost(BATCH_ID);

        // Assert
        assertThat(result.inputs().get(0).lineCost()).isEqualByComparingTo("2400");
        assertThat(result.inputs().get(1).unitCost()).isNull();
        assertThat(result.inputs().get(1).lineCost()).isNull();
        assertThat(result.uncostedInputs()).isEqualTo(1);
        assertThat(result.materialCost()).isNull();
        assertThat(result.unitCost()).isNull();
        assertThat(result.marginPercent()).isNull();
    }

    @Test
    void getBatchCost_costNotDivisible_roundsTheCostPerUnitToTwoDecimals() {
        // Arrange: 3 soaps for 0.833 kg of oil at 1 200 Ar = 999.60 Ar
        var batch = planned("3", input(OIL_ID, "0.833"));
        batch.complete(FROZEN, NOW);
        arrange(batch, "8500", "1200.00", "12000.00");

        // Act
        var result = sut.getBatchCost(BATCH_ID);

        // Assert
        assertThat(result.materialCost()).isEqualByComparingTo("999.60");
        assertThat(result.unitCost()).isEqualByComparingTo("333.20");
    }

    @Test
    void getBatchCost_outputWithoutSalePrice_returnsTheCostWithoutMargin() {
        // Arrange
        var batch = planned("10", input(OIL_ID, "1"));
        batch.complete(FROZEN, NOW);
        arrange(batch, null, "1200.00", "12000.00");

        // Act
        var result = sut.getBatchCost(BATCH_ID);

        // Assert
        assertThat(result.unitCost()).isEqualByComparingTo("120.00");
        assertThat(result.salePrice()).isNull();
        assertThat(result.marginPercent()).isNull();
    }

    @Test
    void getBatchCost_unknownBatch_throwsNotFound() {
        // Arrange
        when(productionBatchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getBatchCost(BATCH_ID)).isInstanceOf(NotFoundException.class);
    }

    /** Batch, output sold at {@code price}, oil and base at their current purchase cost. */
    private void arrange(ProductionBatch batch, @Nullable String price, String oilCost, @Nullable String baseCost) {
        when(productionBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        var output = article(OUTPUT_ID, "Savon noir 100 g", ArticleType.FINISHED, null, price);
        when(articleService.getArticleById(OUTPUT_ID)).thenReturn(output);
        var oil = article(OIL_ID, "Huile de coco", ArticleType.RAW_MATERIAL, oilCost, null);
        var base = article(BASE_ID, "Base savon", ArticleType.RAW_MATERIAL, baseCost, null);
        for (var input : batch.getInputs()) {
            var article = input.getArticleId().equals(OIL_ID) ? oil : base;
            when(articleService.getArticleById(input.getArticleId())).thenReturn(article);
        }
    }

    private static ProductionBatch planned(String quantity, ProductionInput... inputs) {
        var details = new ProductionBatchDetails(OUTPUT_ID, new BigDecimal(quantity), OPERATOR_ID, List.of(inputs));
        return ProductionBatch.plan("LOT-00001", details);
    }

    private static ProductionInput input(UUID articleId, String quantity) {
        return new ProductionInput(articleId, new BigDecimal(quantity));
    }

    private static ArticleRDto article(
            UUID id, String name, ArticleType type, @Nullable String cost, @Nullable String price) {
        var finished = type == ArticleType.FINISHED;
        return new ArticleRDto(
                id,
                "ART-" + name.length(),
                name,
                null,
                type,
                null,
                finished ? ArticleUnit.PIECE : ArticleUnit.KILOGRAM,
                cost == null ? null : new BigDecimal(cost),
                price == null ? null : new BigDecimal(price),
                BigDecimal.ZERO,
                null,
                true,
                NOW,
                NOW,
                0);
    }
}
