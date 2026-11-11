package org.vvs.valimbavaka.feature.commission.service.impl;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.commission.domain.Commission;
import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.domain.CommissionTerms;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.feature.commission.mapper.CommissionMapper;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRepository;
import org.vvs.valimbavaka.feature.commission.service.CommissionRateResolver;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class CommissionServiceImpl implements CommissionService {

    private static final Logger log = LoggerFactory.getLogger(CommissionServiceImpl.class);

    static final String RESOURCE_TYPE = "COMMISSION";

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "businessDate", "sourceNumber", "sellerName", "amount");
    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);
    private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

    private final CommissionRepository commissionRepository;
    private final CommissionMapper commissionMapper;
    private final CommissionRateResolver rateResolver;
    private final UserService userService;
    private final JournalService journalService;

    public CommissionServiceImpl(
            CommissionRepository commissionRepository,
            CommissionMapper commissionMapper,
            CommissionRateResolver rateResolver,
            UserService userService,
            JournalService journalService) {
        this.commissionRepository = commissionRepository;
        this.commissionMapper = commissionMapper;
        this.rateResolver = rateResolver;
        this.userService = userService;
        this.journalService = journalService;
    }

    @Override
    @Transactional
    public Optional<CommissionRDto> calculate(CommissionSource source) {
        var existing = commissionRepository.findBySourceTypeAndSourceId(source.type(), source.id());
        if (existing.isPresent()) {
            return existing.map(commissionMapper::toRDto);
        }
        if (!userService.isActiveSeller(source.sellerId())) {
            log.info("Opération {} : le compte crédité n'est pas une vendeuse, aucune commission", source.number());
            return Optional.empty();
        }
        var applicable = rateResolver.resolve(source.sellerId(), source.salesChannelId(), source.salonServiceId());
        var terms = new CommissionTerms(applicable.rate(), applicable.base(), applicable.source(), applicable.ruleId());
        var commission = commissionRepository.save(Commission.calculate(source, sellerName(source), terms));
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, commission.getId(), description(commission));
        log.info("Commission {} calculée pour {}", commission.getId(), source.number());
        return Optional.of(commission).map(commissionMapper::toRDto);
    }

    @Override
    @Transactional
    public void cancelBySource(CommissionSourceType sourceType, UUID sourceId) {
        commissionRepository
                .findBySourceTypeAndSourceId(sourceType, sourceId)
                .filter(commission -> commission.getStatus() != CommissionStatus.CANCELLED)
                .ifPresent(commission -> {
                    var wasValidated = commission.getStatus() == CommissionStatus.VALIDATED;
                    commission.cancel();
                    var description = "Commission %s Ar annulée avec l'opération %s%s"
                            .formatted(
                                    commission.getAmount().toPlainString(),
                                    commission.getSourceNumber(),
                                    wasValidated ? " (déjà validée : versement à corriger)" : "");
                    journalService.record(JournalAction.CANCEL, RESOURCE_TYPE, commission.getId(), description);
                    log.info("Commission {} annulée avec {}", commission.getId(), commission.getSourceNumber());
                });
    }

    @Override
    public PageResponse<CommissionRDto> getCommissions(
            PageQuery query, @Nullable UUID sellerId, @Nullable LocalDate from, @Nullable LocalDate to) {
        var start = from != null ? from : EARLIEST;
        var end = to != null ? to : LATEST;
        if (start.isAfter(end)) {
            throw new BusinessException("La date de début doit précéder la date de fin.");
        }
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(
                commissionRepository.search(sellerId, start, end, pageable).map(commissionMapper::toRDto));
    }

    @Override
    public Optional<CommissionRDto> findBySource(CommissionSourceType sourceType, UUID sourceId) {
        return commissionRepository
                .findBySourceTypeAndSourceId(sourceType, sourceId)
                .map(commissionMapper::toRDto);
    }

    private String sellerName(CommissionSource source) {
        var seller = userService.getUserById(source.sellerId());
        return (seller.firstName() + " " + seller.lastName()).strip();
    }

    /** E.g. {@code Commission VTE-0042 · Miora Rakoto · 4.00 % de 25500.00 Ar (CA) = 1020.00 Ar}. */
    private static String description(Commission commission) {
        return "Commission %s · %s · %s %% de %s Ar (%s) = %s Ar"
                .formatted(
                        commission.getSourceNumber(),
                        commission.getSellerName(),
                        commission.getRate().toPlainString(),
                        commission.getBaseAmount().toPlainString(),
                        commission.getBase() == CommissionBase.MARGIN ? "marge" : "CA",
                        commission.getAmount().toPlainString());
    }
}
