package org.vvs.valimbavaka.feature.sale.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleFilter;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryRDto;
import org.vvs.valimbavaka.feature.sale.repository.SaleQueryRepository;
import org.vvs.valimbavaka.feature.sale.service.SaleQueryService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class SaleQueryServiceImpl implements SaleQueryService {

    /** Business day of the shop: from midnight to midnight in Madagascar (same zone as the opening, JR-01). */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "number", "total");

    private final SaleQueryRepository saleQueryRepository;
    private final SaleSummaryAssembler summaryAssembler;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;
    private final Clock clock;

    SaleQueryServiceImpl(
            SaleQueryRepository saleQueryRepository,
            SaleSummaryAssembler summaryAssembler,
            UserService userService,
            AuditorAware<UUID> auditorAware,
            Clock clock) {
        this.saleQueryRepository = saleQueryRepository;
        this.summaryAssembler = summaryAssembler;
        this.userService = userService;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    @Override
    public PageResponse<SaleSummaryRDto> getSales(PageQuery query, SaleFilter filter) {
        var requested = filter.businessDate();
        var day = requested != null ? requested : LocalDate.now(clock.withZone(BUSINESS_ZONE));
        var scoped = ownSellerId().map(filter::forSeller).orElse(filter);
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        var ids = saleQueryRepository.searchIds(day, scoped.salesChannelId(), scoped.sellerId(), pageable);
        List<Sale> sales = ids.hasContent() ? saleQueryRepository.findWithLinesByIdIn(ids.getContent()) : List.of();
        return new PageResponse<>(
                summaryAssembler.toRDtos(ids.getContent(), sales),
                ids.getNumber() + 1,
                ids.getSize(),
                ids.getTotalElements(),
                ids.getTotalPages());
    }

    /** The caller's id when she is an active seller: she only reads the sales credited to her. */
    private Optional<UUID> ownSellerId() {
        return auditorAware.getCurrentAuditor().filter(userService::isActiveSeller);
    }
}
