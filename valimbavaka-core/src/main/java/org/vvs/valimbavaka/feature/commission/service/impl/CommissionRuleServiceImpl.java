package org.vvs.valimbavaka.feature.commission.service.impl;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRule;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRuleDetails;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleWDto;
import org.vvs.valimbavaka.feature.commission.mapper.CommissionRuleMapper;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRuleRepository;
import org.vvs.valimbavaka.feature.commission.service.CommissionRuleService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class CommissionRuleServiceImpl implements CommissionRuleService {

    private static final Logger log = LoggerFactory.getLogger(CommissionRuleServiceImpl.class);

    private static final String RESOURCE_TYPE = "COMMISSION_RULE";
    private static final String NAME_ALREADY_USED = "Une règle de commission avec ce nom existe déjà.";
    private static final String CHANNEL_ALREADY_COVERED =
            "Ce canal de vente a déjà une règle de commission active : désactivez-la d'abord.";
    private static final String STALE_VERSION =
            "La règle a été modifiée entre-temps : rechargez la fiche puis réessayez.";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "rate");

    private final CommissionRuleRepository ruleRepository;
    private final CommissionRuleMapper ruleMapper;
    private final ReferentialService referentialService;
    private final JournalService journalService;

    public CommissionRuleServiceImpl(
            CommissionRuleRepository ruleRepository,
            CommissionRuleMapper ruleMapper,
            ReferentialService referentialService,
            JournalService journalService) {
        this.ruleRepository = ruleRepository;
        this.ruleMapper = ruleMapper;
        this.referentialService = referentialService;
        this.journalService = journalService;
    }

    @Override
    public PageResponse<CommissionRuleRDto> getRules(
            PageQuery query, @Nullable UUID salesChannelId, @Nullable Boolean active) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(ruleRepository
                .search(query.searchTerm(), salesChannelId, active, pageable)
                .map(ruleMapper::toRDto));
    }

    @Override
    public CommissionRuleRDto getRuleById(UUID ruleId) {
        return ruleMapper.toRDto(findRule(ruleId));
    }

    @Override
    @Transactional
    public CommissionRuleRDto createRule(CommissionRuleWDto request) {
        requireActiveChannel(request.salesChannelId());
        if (ruleRepository.existsByNameIgnoreCase(request.name().strip())) {
            throw new ConflictException(NAME_ALREADY_USED);
        }
        requireNoOtherActiveRule(request.salesChannelId(), null);
        CommissionRule saved = ruleRepository.save(CommissionRule.create(toDetails(request)));
        String description = "Création de la règle de commission " + label(saved);
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, saved.getId(), description);
        log.info("Règle de commission {} créée", saved.getId());
        return ruleMapper.toRDto(saved);
    }

    @Override
    @Transactional
    public CommissionRuleRDto updateRule(UUID ruleId, CommissionRuleWDto request) {
        CommissionRule rule = findRule(ruleId);
        if (request.version() != null && request.version() != rule.getVersion()) {
            throw new ConflictException(STALE_VERSION);
        }
        if (!request.salesChannelId().equals(rule.getSalesChannelId())) {
            requireActiveChannel(request.salesChannelId());
        }
        if (ruleRepository.existsByNameIgnoreCaseAndIdNot(request.name().strip(), ruleId)) {
            throw new ConflictException(NAME_ALREADY_USED);
        }
        if (rule.isActive()) {
            requireNoOtherActiveRule(request.salesChannelId(), ruleId);
        }
        if (rule.update(toDetails(request))) {
            String description = "Modification de la règle de commission " + label(rule);
            journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, ruleId, description);
        }
        return ruleMapper.toRDto(rule);
    }

    @Override
    @Transactional
    public CommissionRuleRDto setActive(UUID ruleId, boolean active) {
        CommissionRule rule = findRule(ruleId);
        if (rule.isActive() == active) {
            return ruleMapper.toRDto(rule);
        }
        if (active) {
            requireNoOtherActiveRule(rule.getSalesChannelId(), ruleId);
            rule.activate();
        } else {
            rule.deactivate();
        }
        String action = active ? "Activation" : "Désactivation";
        String description = action + " de la règle de commission " + label(rule);
        journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, ruleId, description);
        log.info("Règle de commission {} {}", ruleId, active ? "activée" : "désactivée");
        return ruleMapper.toRDto(rule);
    }

    private CommissionRule findRule(UUID ruleId) {
        return ruleRepository
                .findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Règle de commission introuvable : " + ruleId));
    }

    private void requireActiveChannel(UUID salesChannelId) {
        if (!referentialService.isActiveSalesChannel(salesChannelId)) {
            throw new BusinessException("Canal de vente introuvable : " + salesChannelId);
        }
    }

    /** One active rule per channel: the rule {@code ruleId} itself (update, activation) does not count. */
    private void requireNoOtherActiveRule(UUID salesChannelId, @Nullable UUID ruleId) {
        var existing = ruleRepository.findBySalesChannelIdAndActiveTrue(salesChannelId);
        if (existing.isPresent() && !existing.get().getId().equals(ruleId)) {
            throw new ConflictException(CHANNEL_ALREADY_COVERED);
        }
    }

    private static String label(CommissionRule rule) {
        return rule.getName() + " (" + rule.getRate().toPlainString() + " %)";
    }

    private static CommissionRuleDetails toDetails(CommissionRuleWDto request) {
        return new CommissionRuleDetails(request.name(), request.salesChannelId(), request.rate(), request.base());
    }
}
