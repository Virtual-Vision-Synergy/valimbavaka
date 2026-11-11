package org.vvs.valimbavaka.feature.commission.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.dto.CommissionFollowUpRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.feature.commission.mapper.CommissionMapper;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRepository;
import org.vvs.valimbavaka.feature.commission.repository.SellerStatusTotal;
import org.vvs.valimbavaka.feature.commission.service.CommissionFollowUpService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class CommissionFollowUpServiceImpl implements CommissionFollowUpService {

    /** Business month of the shop: Madagascar time, like the business days stored on the commissions. */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "businessDate", "sourceNumber", "sellerName", "amount");
    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CommissionRepository commissionRepository;
    private final CommissionMapper commissionMapper;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;
    private final Clock clock;

    public CommissionFollowUpServiceImpl(
            CommissionRepository commissionRepository,
            CommissionMapper commissionMapper,
            UserService userService,
            AuditorAware<UUID> auditorAware,
            Clock clock) {
        this.commissionRepository = commissionRepository;
        this.commissionMapper = commissionMapper;
        this.userService = userService;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    @Override
    public CommissionFollowUpRDto getFollowUp(@Nullable UUID sellerId, @Nullable YearMonth month) {
        var seller = resolveSeller(sellerId);
        var period = month != null ? month : YearMonth.now(clock.withZone(BUSINESS_ZONE));
        var from = period.atDay(1);
        var to = period.atEndOfMonth();
        var totals = commissionRepository.totalsBySellerAndStatus(List.of(seller.id()), from, to);
        var revenue = commissionRepository.sumRevenue(seller.id(), from, to);
        var revenueOfMonth = revenue != null ? revenue : ZERO_AMOUNT;
        var target = seller.monthlyTarget();
        return new CommissionFollowUpRDto(
                seller.id(),
                fullName(seller),
                period.toString(),
                from,
                to,
                totals.stream().mapToLong(SellerStatusTotal::getCommissionCount).sum(),
                revenueOfMonth,
                amountOf(totals, null),
                amountOf(totals, CommissionStatus.CALCULATED),
                amountOf(totals, CommissionStatus.VALIDATED),
                target,
                progress(revenueOfMonth, target));
    }

    @Override
    public PageResponse<CommissionRDto> getLines(PageQuery query, @Nullable UUID sellerId, @Nullable YearMonth month) {
        var seller = resolveSeller(sellerId);
        var period = month != null ? month : YearMonth.now(clock.withZone(BUSINESS_ZONE));
        var pageable = query.toPageable(SORT_FIELDS, "businessDate", SortDirection.DESC);
        return PageResponse.from(commissionRepository
                .search(seller.id(), period.atDay(1), period.atEndOfMonth(), pageable)
                .map(commissionMapper::toRDto));
    }

    /**
     * A seller always reads herself, whatever {@code requestedId} says (VTE-07 rule); the administrator must name a
     * seller.
     */
    private UserRDto resolveSeller(@Nullable UUID requestedId) {
        var own = auditorAware.getCurrentAuditor().filter(userService::isActiveSeller);
        @Nullable UUID sellerId = own.isPresent() ? own.get() : requestedId;
        if (sellerId == null) {
            throw new BusinessException("La vendeuse est obligatoire.");
        }
        var seller = userService.getUserById(sellerId);
        if (seller.role().name() != RoleName.SELLER) {
            throw new NotFoundException("Vendeuse introuvable : " + seller.id());
        }
        return seller;
    }

    private static String fullName(UserRDto seller) {
        return (seller.firstName() + " " + seller.lastName()).strip();
    }

    /** Total of one status, or of all statuses when {@code status} is null. */
    private static BigDecimal amountOf(List<SellerStatusTotal> totals, @Nullable CommissionStatus status) {
        return totals.stream()
                .filter(total -> status == null || total.getStatus() == status)
                .map(SellerStatusTotal::getTotalAmount)
                .reduce(ZERO_AMOUNT, BigDecimal::add);
    }

    private static @Nullable BigDecimal progress(BigDecimal revenue, @Nullable BigDecimal target) {
        if (target == null || target.signum() <= 0) {
            return null;
        }
        return revenue.multiply(HUNDRED).divide(target, 1, RoundingMode.HALF_UP);
    }
}
