package org.vvs.valimbavaka.feature.promotion.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionStatus;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionWDto;
import org.vvs.valimbavaka.feature.promotion.mapper.PromotionMapper;
import org.vvs.valimbavaka.feature.promotion.repository.PromotionRepository;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionServiceImpl.class);

    private static final String RESOURCE_TYPE = "PROMOTION";
    private static final String CODE_ALREADY_USED = "Ce code promo est déjà utilisé par une autre promotion.";
    private static final Set<String> SORT_FIELDS = Set.of("startsOn", "endsOn", "name", "createdAt");

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final PromotionRules promotionRules;
    private final JournalService journalService;
    private final Clock clock;

    public PromotionServiceImpl(
            PromotionRepository promotionRepository,
            PromotionMapper promotionMapper,
            PromotionRules promotionRules,
            JournalService journalService,
            Clock clock) {
        this.promotionRepository = promotionRepository;
        this.promotionMapper = promotionMapper;
        this.promotionRules = promotionRules;
        this.journalService = journalService;
        this.clock = clock;
    }

    @Override
    public PageResponse<PromotionRDto> getPromotions(PageQuery query, @Nullable PromotionStatus status) {
        var today = today();
        var pageable = query.toPageable(SORT_FIELDS, "startsOn", SortDirection.DESC);
        var statusFilter = status == null ? PromotionRepository.ANY_STATUS : status.name();
        return PageResponse.from(promotionRepository
                .search(query.searchTerm(), statusFilter, today, pageable)
                .map(promotion ->
                        promotionMapper.toRDto(promotion, promotion.getPeriod().statusOn(today))));
    }

    @Override
    public PromotionRDto getPromotionById(UUID promotionId) {
        var promotion = promotionRepository
                .findById(promotionId)
                .orElseThrow(() -> new NotFoundException("Promotion introuvable : " + promotionId));
        return toRDto(promotion);
    }

    @Override
    @Transactional
    public PromotionRDto createPromotion(PromotionWDto request) {
        var details = toDetails(request);
        promotionRules.check(details, today());
        var code = details.code();
        if (code != null && promotionRepository.existsByCode(code)) {
            throw new ConflictException(CODE_ALREADY_USED);
        }
        Promotion saved = promotionRepository.save(Promotion.create(details));
        journalService.record(
                JournalAction.CREATE, RESOURCE_TYPE, saved.getId(), "Création de la promotion " + saved.getName());
        log.info("Promotion {} créée", saved.getId());
        return toRDto(saved);
    }

    private PromotionRDto toRDto(Promotion promotion) {
        return promotionMapper.toRDto(promotion, promotion.getPeriod().statusOn(today()));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(PromotionRules.BUSINESS_ZONE));
    }

    private static PromotionDetails toDetails(PromotionWDto request) {
        var requestedTargets = request.targetIds();
        var targetIds = requestedTargets == null ? Set.<UUID>of() : Set.copyOf(requestedTargets);
        return new PromotionDetails(
                request.name(),
                request.description(),
                request.kind(),
                request.value(),
                request.code(),
                request.scope(),
                targetIds,
                new PromotionPeriod(request.startsOn(), request.endsOn()),
                Set.copyOf(request.channelIds()),
                request.usageLimit());
    }
}
