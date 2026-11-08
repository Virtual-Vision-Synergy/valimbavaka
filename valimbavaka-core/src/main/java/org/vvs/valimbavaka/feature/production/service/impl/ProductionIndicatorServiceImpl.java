package org.vvs.valimbavaka.feature.production.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.dto.ProductionIndicatorsRDto;
import org.vvs.valimbavaka.feature.production.repository.ProductionBatchRepository;
import org.vvs.valimbavaka.feature.production.service.ProductionIndicatorService;

/**
 * Indicators of the current month (PRD-04). A batch counts in the month of its completion (PRD-02 rule: status
 * COMPLETED, dated by {@code completedAt}); the material cost leaves out the batches with an input without cost
 * instead of counting that input for 0.
 */
@Service
@Transactional(readOnly = true)
public class ProductionIndicatorServiceImpl implements ProductionIndicatorService {

    /** Months of the business (Madagascar), whatever the server clock zone. */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private static final int COST_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ProductionBatchRepository productionBatchRepository;
    private final Clock clock;

    ProductionIndicatorServiceImpl(ProductionBatchRepository productionBatchRepository, Clock clock) {
        this.productionBatchRepository = productionBatchRepository;
        this.clock = clock;
    }

    @Override
    public ProductionIndicatorsRDto getIndicators() {
        var month = YearMonth.now(clock.withZone(BUSINESS_ZONE));
        var start = startOf(month);
        var totals = totalsOf(productionBatchRepository.findCompletedBetween(start, startOf(month.plusMonths(1))));
        var previousStart = startOf(month.minusMonths(1));
        var previousUnits = productionBatchRepository.sumCompletedQuantityBetween(previousStart, start);
        return new ProductionIndicatorsRDto(
                month.toString(),
                totals.batches(),
                productionBatchRepository.countByStatus(ProductionBatchStatus.IN_PROGRESS),
                totals.units(),
                previousUnits,
                trendPercent(totals.units(), previousUnits),
                totals.materialCost(),
                perUnit(totals.materialCost(), totals.costedUnits()),
                totals.uncostedBatches());
    }

    private static Totals totalsOf(List<ProductionBatch> batches) {
        var units = BigDecimal.ZERO;
        var costedUnits = BigDecimal.ZERO;
        var materialCost = BigDecimal.ZERO;
        long uncosted = 0;
        for (var batch : batches) {
            units = units.add(batch.getQuantity());
            var cost = materialCostOf(batch);
            if (cost == null) {
                uncosted++;
            } else {
                costedUnits = costedUnits.add(batch.getQuantity());
                materialCost = materialCost.add(cost);
            }
        }
        return new Totals(batches.size(), units, costedUnits, materialCost, uncosted);
    }

    /** Sum of {@code quantity x unitCost} of the inputs; null as soon as one input has no frozen cost. */
    private static @Nullable BigDecimal materialCostOf(ProductionBatch batch) {
        var total = BigDecimal.ZERO;
        for (var input : batch.getInputs()) {
            var unitCost = input.getUnitCost();
            if (unitCost == null) {
                return null;
            }
            total = total.add(input.getQuantity().multiply(unitCost));
        }
        return total;
    }

    private static @Nullable Integer trendPercent(BigDecimal units, BigDecimal previousUnits) {
        if (previousUnits.signum() == 0) {
            return null;
        }
        var change = units.subtract(previousUnits).multiply(HUNDRED);
        return change.divide(previousUnits, 0, RoundingMode.HALF_UP).intValue();
    }

    private static @Nullable BigDecimal perUnit(BigDecimal cost, BigDecimal units) {
        return units.signum() == 0 ? null : cost.divide(units, COST_SCALE, RoundingMode.HALF_UP);
    }

    private static Instant startOf(YearMonth month) {
        return month.atDay(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private record Totals(
            long batches, BigDecimal units, BigDecimal costedUnits, BigDecimal materialCost, long uncostedBatches) {}
}
