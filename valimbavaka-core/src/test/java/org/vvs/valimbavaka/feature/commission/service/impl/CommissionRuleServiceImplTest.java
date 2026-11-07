package org.vvs.valimbavaka.feature.commission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRule;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRuleDetails;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleWDto;
import org.vvs.valimbavaka.feature.commission.mapper.CommissionRuleMapper;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRuleRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;

@ExtendWith(MockitoExtension.class)
class CommissionRuleServiceImplTest {

    private static final UUID RULE_ID = UUID.randomUUID();
    private static final UUID SHOP_ID = UUID.randomUUID();
    private static final UUID FACEBOOK_ID = UUID.randomUUID();

    @Mock
    private CommissionRuleRepository ruleRepository;

    @Mock
    private CommissionRuleMapper ruleMapper;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private JournalService journalService;

    private CommissionRuleServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CommissionRuleServiceImpl(ruleRepository, ruleMapper, referentialService, journalService);
    }

    @Test
    void createRule_newNameOnFreeChannel_savesNormalizedActiveRuleAndTracesInJournal() {
        // Arrange
        when(referentialService.isActiveSalesChannel(SHOP_ID)).thenReturn(true);
        when(ruleRepository.existsByNameIgnoreCase("Boutique standard")).thenReturn(false);
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.empty());
        when(ruleRepository.save(any(CommissionRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sut.createRule(request("  Boutique standard ", SHOP_ID, "3.5", null));

        // Assert
        var saved = ArgumentCaptor.forClass(CommissionRule.class);
        verify(ruleRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Boutique standard");
        assertThat(saved.getValue().getRate()).isEqualByComparingTo("3.50");
        assertThat(saved.getValue().getRate().scale()).isEqualTo(2);
        assertThat(saved.getValue().getBase()).isEqualTo(CommissionBase.REVENUE);
        assertThat(saved.getValue().isActive()).isTrue();
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("COMMISSION_RULE"),
                        any(),
                        eq("Création de la règle de commission Boutique standard (3.50 %)"));
    }

    @Test
    void createRule_nameAlreadyUsed_throwsConflictWithoutSaving() {
        // Arrange
        when(referentialService.isActiveSalesChannel(SHOP_ID)).thenReturn(true);
        when(ruleRepository.existsByNameIgnoreCase("Boutique standard")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.createRule(request("Boutique standard", SHOP_ID, "3", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("nom");
        verify(ruleRepository, never()).save(any());
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void createRule_channelAlreadyCoveredByActiveRule_throwsConflictWithoutSaving() {
        // Arrange
        var existing = mock(CommissionRule.class);
        when(existing.getId()).thenReturn(UUID.randomUUID());
        when(referentialService.isActiveSalesChannel(SHOP_ID)).thenReturn(true);
        when(ruleRepository.existsByNameIgnoreCase("Boutique promo")).thenReturn(false);
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> sut.createRule(request("Boutique promo", SHOP_ID, "4", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà une règle de commission active");
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createRule_unknownOrInactiveChannel_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveSalesChannel(SHOP_ID)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.createRule(request("Boutique standard", SHOP_ID, "3", null)))
                .isInstanceOf(BusinessException.class)
                .isNotInstanceOf(ConflictException.class)
                .hasMessageContaining("Canal de vente introuvable");
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void updateRule_staleVersion_throwsConflictWithoutChange() {
        // Arrange
        var rule = shopRule();
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

        // Act + Assert
        assertThatThrownBy(() -> sut.updateRule(RULE_ID, request("Boutique standard", SHOP_ID, "9", 7L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modifiée entre-temps");
        assertThat(rule.getRate()).isEqualByComparingTo("3.00");
    }

    @Test
    void updateRule_newRate_appliesItAndTracesUpdate() {
        // Arrange
        var rule = shopRule();
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsByNameIgnoreCaseAndIdNot("Boutique standard", RULE_ID)).thenReturn(false);
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.empty());

        // Act
        sut.updateRule(RULE_ID, request("Boutique standard", SHOP_ID, "4.25", 0L));

        // Assert
        assertThat(rule.getRate()).isEqualByComparingTo("4.25");
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "COMMISSION_RULE",
                        RULE_ID,
                        "Modification de la règle de commission Boutique standard (4.25 %)");
    }

    @Test
    void updateRule_sameValues_doesNotTraceAnything() {
        // Arrange
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(shopRule()));
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.empty());

        // Act
        sut.updateRule(RULE_ID, request(" Boutique standard ", SHOP_ID, "3", null));

        // Assert
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void updateRule_moveToChannelCoveredByAnotherActiveRule_throwsConflict() {
        // Arrange
        var other = mock(CommissionRule.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(shopRule()));
        when(referentialService.isActiveSalesChannel(FACEBOOK_ID)).thenReturn(true);
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(FACEBOOK_ID)).thenReturn(Optional.of(other));

        // Act + Assert
        assertThatThrownBy(() -> sut.updateRule(RULE_ID, request("Boutique standard", FACEBOOK_ID, "3", null)))
                .isInstanceOf(ConflictException.class);
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void setActive_sameState_isIdempotentAndTracesNothing() {
        // Arrange
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(shopRule()));

        // Act
        sut.setActive(RULE_ID, true);

        // Assert
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void setActive_deactivate_marksInactiveAndTracesUpdate() {
        // Arrange
        var rule = shopRule();
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

        // Act
        sut.setActive(RULE_ID, false);

        // Assert
        assertThat(rule.isActive()).isFalse();
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "COMMISSION_RULE",
                        RULE_ID,
                        "Désactivation de la règle de commission Boutique standard (3.00 %)");
    }

    @Test
    void setActive_activateWhileChannelCoveredByAnotherRule_throwsConflictAndStaysInactive() {
        // Arrange
        var rule = shopRule();
        rule.deactivate();
        var other = mock(CommissionRule.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.of(other));

        // Act + Assert
        assertThatThrownBy(() -> sut.setActive(RULE_ID, true)).isInstanceOf(ConflictException.class);
        assertThat(rule.isActive()).isFalse();
    }

    @Test
    void getRuleById_unknownId_throwsNotFound() {
        // Arrange
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getRuleById(RULE_ID)).isInstanceOf(NotFoundException.class);
    }

    private static CommissionRule shopRule() {
        return CommissionRule.create(
                new CommissionRuleDetails("Boutique standard", SHOP_ID, new BigDecimal("3"), CommissionBase.REVENUE));
    }

    private static CommissionRuleWDto request(String name, UUID channelId, String rate, Long version) {
        return new CommissionRuleWDto(name, channelId, new BigDecimal(rate), CommissionBase.REVENUE, version);
    }
}
