package org.vvs.valimbavaka.feature.commission.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.commission.domain.Commission;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.domain.SellerCommissionStatus;
import org.vvs.valimbavaka.feature.commission.dto.CommissionValidationRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionValidationWDto;
import org.vvs.valimbavaka.feature.commission.dto.SellerCommissionRDto;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRepository;
import org.vvs.valimbavaka.feature.commission.repository.SellerKey;
import org.vvs.valimbavaka.feature.commission.repository.SellerStatusTotal;
import org.vvs.valimbavaka.feature.commission.service.CommissionValidationService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@Service
@Transactional(readOnly = true)
public class CommissionValidationServiceImpl implements CommissionValidationService {

    private static final String PERIOD_INVERTED = "La date de début doit précéder la date de fin.";
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");

    private final CommissionRepository commissionRepository;
    private final JournalService journalService;
    private final Clock clock;

    public CommissionValidationServiceImpl(
            CommissionRepository commissionRepository, JournalService journalService, Clock clock) {
        this.commissionRepository = commissionRepository;
        this.journalService = journalService;
        this.clock = clock;
    }

    @Override
    public PageResponse<SellerCommissionRDto> getSellerSummaries(PageQuery query, LocalDate from, LocalDate to) {
        checkPeriod(from, to);
        var pageable = PageRequest.of(query.page() - 1, query.size());
        var sellers = commissionRepository.searchSellers(from, to, pageable);
        var ids = sellers.map(SellerKey::getSellerId).getContent();
        var totals = totalsOf(ids, from, to);
        return PageResponse.from(
                sellers.map(seller -> summary(seller, totals.getOrDefault(seller.getSellerId(), Map.of()))));
    }

    @Override
    @Transactional
    public CommissionValidationRDto validate(CommissionValidationWDto request) {
        checkPeriod(request.from(), request.to());
        var pending = commissionRepository.findBySellerIdAndStatusAndBusinessDateBetween(
                request.sellerId(), CommissionStatus.CALCULATED, request.from(), request.to());
        if (pending.isEmpty()) {
            throw emptyValidation(request);
        }
        var now = Instant.now(clock);
        pending.forEach(commission -> validate(commission, now));
        var total = pending.stream().map(Commission::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CommissionValidationRDto(
                request.sellerId(),
                pending.getFirst().getSellerName(),
                request.from(),
                request.to(),
                pending.size(),
                total);
    }

    private void validate(Commission commission, Instant now) {
        commission.validate(now);
        var description = "Commission %s · %s validée : %s Ar"
                .formatted(
                        commission.getSourceNumber(),
                        commission.getSellerName(),
                        commission.getAmount().toPlainString());
        journalService.record(
                JournalAction.UPDATE, CommissionServiceImpl.RESOURCE_TYPE, commission.getId(), description);
    }

    /** Nothing to validate: unknown seller or empty period (404), or everything already validated (409). */
    private BusinessException emptyValidation(CommissionValidationWDto request) {
        var hasCommissions = commissionRepository.existsBySellerIdAndBusinessDateBetweenAndStatusNot(
                request.sellerId(), request.from(), request.to(), CommissionStatus.CANCELLED);
        return hasCommissions
                ? new ConflictException("Les commissions de cette vendeuse sont déjà validées sur cette période.")
                : new NotFoundException("Aucune commission pour cette vendeuse sur cette période.");
    }

    private Map<UUID, Map<CommissionStatus, SellerStatusTotal>> totalsOf(
            Collection<UUID> sellerIds, LocalDate from, LocalDate to) {
        var bySeller = new HashMap<UUID, Map<CommissionStatus, SellerStatusTotal>>();
        if (sellerIds.isEmpty()) {
            return bySeller;
        }
        for (var total : commissionRepository.totalsBySellerAndStatus(sellerIds, from, to)) {
            bySeller.computeIfAbsent(total.getSellerId(), id -> new EnumMap<>(CommissionStatus.class))
                    .put(total.getStatus(), total);
        }
        return bySeller;
    }

    private static SellerCommissionRDto summary(SellerKey seller, Map<CommissionStatus, SellerStatusTotal> totals) {
        var toValidate = totals.get(CommissionStatus.CALCULATED);
        var validated = totals.get(CommissionStatus.VALIDATED);
        var status = toValidate == null ? SellerCommissionStatus.VALIDATED : SellerCommissionStatus.TO_VALIDATE;
        return new SellerCommissionRDto(
                seller.getSellerId(),
                seller.getSellerName(),
                status,
                countOf(toValidate),
                amountOf(toValidate),
                countOf(validated),
                amountOf(validated));
    }

    private static long countOf(@Nullable SellerStatusTotal total) {
        return total == null ? 0 : total.getCommissionCount();
    }

    private static BigDecimal amountOf(@Nullable SellerStatusTotal total) {
        return total == null ? ZERO_AMOUNT : total.getTotalAmount();
    }

    private static void checkPeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(PERIOD_INVERTED);
        }
    }
}
