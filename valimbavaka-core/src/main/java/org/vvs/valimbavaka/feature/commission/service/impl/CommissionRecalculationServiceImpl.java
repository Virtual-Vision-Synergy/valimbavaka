package org.vvs.valimbavaka.feature.commission.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.commission.domain.Commission;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.domain.CommissionTerms;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRecalculationRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRecalculationWDto;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRepository;
import org.vvs.valimbavaka.feature.commission.service.CommissionRateResolver;
import org.vvs.valimbavaka.feature.commission.service.CommissionRecalculationService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.service.SaleService;

@Service
@Transactional(readOnly = true)
public class CommissionRecalculationServiceImpl implements CommissionRecalculationService {

    private static final Logger log = LoggerFactory.getLogger(CommissionRecalculationServiceImpl.class);

    /** The recalculation is bounded: a year of business days at most. */
    private static final long MAX_DAYS = 366;

    private final CommissionRepository commissionRepository;
    private final CommissionRateResolver rateResolver;
    private final SaleService saleService;
    private final JournalService journalService;

    public CommissionRecalculationServiceImpl(
            CommissionRepository commissionRepository,
            CommissionRateResolver rateResolver,
            SaleService saleService,
            JournalService journalService) {
        this.commissionRepository = commissionRepository;
        this.rateResolver = rateResolver;
        this.saleService = saleService;
        this.journalService = journalService;
    }

    @Override
    @Transactional
    public CommissionRecalculationRDto recalculate(CommissionRecalculationWDto request) {
        checkPeriod(request.from(), request.to());
        var pending = findPending(request);
        var validatedCount = countValidated(request);
        var termsCache = new HashMap<TermsKey, CommissionTerms>();
        var previousAmount = BigDecimal.ZERO;
        var newAmount = BigDecimal.ZERO;
        long updatedCount = 0;
        for (var commission : pending) {
            var previous = commission.getAmount();
            var rate = commission.getRate();
            if (recalculate(commission, termsCache)) {
                updatedCount++;
                trace(commission, previous, rate);
            }
            previousAmount = previousAmount.add(previous);
            newAmount = newAmount.add(commission.getAmount());
        }
        log.info("Recalcul des commissions : {} modifiée(s) sur {}", updatedCount, pending.size());
        return new CommissionRecalculationRDto(
                request.from(),
                request.to(),
                request.sellerId(),
                updatedCount,
                pending.size() - updatedCount,
                validatedCount,
                previousAmount,
                newAmount);
    }

    private List<Commission> findPending(CommissionRecalculationWDto request) {
        return commissionRepository.findByStatusInPeriod(
                CommissionStatus.CALCULATED, request.sellerId(), request.from(), request.to());
    }

    private long countValidated(CommissionRecalculationWDto request) {
        return commissionRepository.countByStatusInPeriod(
                CommissionStatus.VALIDATED, request.sellerId(), request.from(), request.to());
    }

    /** Same rate as the calculation (resolver, then {@code Commission.recalculate}), the purchase cost read again. */
    private boolean recalculate(Commission commission, Map<TermsKey, CommissionTerms> termsCache) {
        var key = TermsKey.of(commission);
        var terms = termsCache.get(key);
        if (terms == null) {
            terms = rateResolver
                    .resolve(key.sellerId(), key.salesChannelId(), key.salonServiceId())
                    .toTerms();
            termsCache.put(key, terms);
        }
        return commission.recalculate(purchaseCost(commission), terms);
    }

    private @Nullable BigDecimal purchaseCost(Commission commission) {
        if (commission.getSourceType() != CommissionSourceType.SALE) {
            return null;
        }
        return saleService.getPurchaseCost(commission.getSourceId());
    }

    /** E.g. {@code Commission VTE-0042 · Miora Rakoto recalculée : 1020.00 → 1275.00 Ar}. */
    private void trace(Commission commission, BigDecimal previousAmount, BigDecimal previousRate) {
        var description = "Commission %s · %s recalculée : %s → %s Ar (taux %s %% → %s %%)"
                .formatted(
                        commission.getSourceNumber(),
                        commission.getSellerName(),
                        previousAmount.toPlainString(),
                        commission.getAmount().toPlainString(),
                        previousRate.toPlainString(),
                        commission.getRate().toPlainString());
        journalService.record(
                JournalAction.UPDATE, CommissionServiceImpl.RESOURCE_TYPE, commission.getId(), description);
    }

    private static void checkPeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("La date de début doit précéder la date de fin.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new BusinessException("La période à recalculer ne peut pas dépasser un an.");
        }
    }

    /** What the rate depends on: two commissions with the same key get the same terms. */
    private record TermsKey(
            UUID sellerId, UUID salesChannelId, @Nullable UUID salonServiceId) {

        static TermsKey of(Commission commission) {
            return new TermsKey(
                    commission.getSellerId(), commission.getSalesChannelId(), commission.getSalonServiceId());
        }
    }
}
