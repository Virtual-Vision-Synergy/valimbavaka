package org.vvs.valimbavaka.feature.commission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.commission.domain.Commission;
import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.domain.CommissionTerms;
import org.vvs.valimbavaka.feature.commission.dto.ApplicableCommissionRDto;
import org.vvs.valimbavaka.feature.commission.mapper.CommissionMapper;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRepository;
import org.vvs.valimbavaka.feature.commission.service.CommissionRateResolver;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

@ExtendWith(MockitoExtension.class)
class CommissionServiceImplTest {

    private static final UUID COMMISSION_ID = UUID.randomUUID();
    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID RULE_ID = UUID.randomUUID();

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private CommissionMapper commissionMapper;

    @Mock
    private CommissionRateResolver rateResolver;

    @Mock
    private UserService userService;

    @Mock
    private JournalService journalService;

    private CommissionServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CommissionServiceImpl(
                commissionRepository, commissionMapper, rateResolver, userService, journalService);
    }

    @Test
    void calculate_activeSeller_savesRevenueTimesResolvedRateAndTracesTheJournal() {
        // Arrange : règle du canal 4 % sur 25 500 Ar
        when(commissionRepository.findBySourceTypeAndSourceId(CommissionSourceType.SALE, SALE_ID))
                .thenReturn(Optional.empty());
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(true);
        when(userService.getUserById(SELLER_ID)).thenReturn(seller());
        when(rateResolver.resolve(SELLER_ID, CHANNEL_ID, null))
                .thenReturn(new ApplicableCommissionRDto(
                        new BigDecimal("4.00"), CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID));
        when(commissionRepository.save(any(Commission.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        // Act
        sut.calculate(sale());

        // Assert
        var saved = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualTo(new BigDecimal("1020.00"));
        assertThat(saved.getValue().getSellerName()).isEqualTo("Miora Rakoto");
        assertThat(saved.getValue().getRuleId()).isEqualTo(RULE_ID);
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("COMMISSION"),
                        eq(COMMISSION_ID),
                        contains("Commission VTE-0042 · Miora Rakoto · 4.00 % de 25500.00 Ar (CA) = 1020.00 Ar"));
    }

    @Test
    void calculate_operationAlreadyCommissioned_returnsTheExistingCommissionWithoutRecalculating() {
        // Arrange
        var existing = Commission.calculate(sale(), "Miora Rakoto", terms());
        when(commissionRepository.findBySourceTypeAndSourceId(CommissionSourceType.SALE, SALE_ID))
                .thenReturn(Optional.of(existing));

        // Act
        sut.calculate(sale());

        // Assert
        verify(commissionMapper).toRDto(existing);
        verify(commissionRepository, never()).save(any(Commission.class));
        verifyNoInteractions(rateResolver, journalService);
    }

    @Test
    void cancelBySource_calculatedCommission_becomesCancelledAndIsJournaled() {
        // Arrange
        var commission = Commission.calculate(sale(), "Miora Rakoto", terms());
        when(commissionRepository.findBySourceTypeAndSourceId(CommissionSourceType.SALE, SALE_ID))
                .thenReturn(Optional.of(commission));

        // Act
        sut.cancelBySource(CommissionSourceType.SALE, SALE_ID);

        // Assert
        assertThat(commission.getStatus()).isEqualTo(CommissionStatus.CANCELLED);
        verify(journalService).record(eq(JournalAction.CANCEL), eq("COMMISSION"), any(), contains("VTE-0042"));
    }

    @Test
    void cancelBySource_alreadyCancelledOrUnknown_doesNothing() {
        // Arrange : une commission déjà annulée, puis une opération sans commission
        var cancelled = Commission.calculate(sale(), "Miora Rakoto", terms());
        cancelled.cancel();
        when(commissionRepository.findBySourceTypeAndSourceId(CommissionSourceType.SALE, SALE_ID))
                .thenReturn(Optional.of(cancelled));
        var unknown = UUID.randomUUID();
        when(commissionRepository.findBySourceTypeAndSourceId(CommissionSourceType.SALE, unknown))
                .thenReturn(Optional.empty());

        // Act
        sut.cancelBySource(CommissionSourceType.SALE, SALE_ID);
        sut.cancelBySource(CommissionSourceType.SALE, unknown);

        // Assert
        verifyNoInteractions(journalService);
    }

    @Test
    void calculate_accountIsNotAnActiveSeller_returnsEmptyAndSavesNothing() {
        // Arrange : administrateur qui enregistre une prestation
        when(commissionRepository.findBySourceTypeAndSourceId(CommissionSourceType.SALE, SALE_ID))
                .thenReturn(Optional.empty());
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(false);

        // Act
        var result = sut.calculate(sale());

        // Assert
        assertThat(result).isEmpty();
        verify(commissionRepository, never()).save(any(Commission.class));
        verifyNoInteractions(rateResolver, journalService);
    }

    @Test
    void getCommissions_fromAfterTo_throwsBusinessException() {
        // Arrange
        var query = new PageQuery(1, 20, null, null, null);
        var from = LocalDate.of(2026, 11, 30);
        var to = LocalDate.of(2026, 11, 1);

        // Act + Assert
        assertThatThrownBy(() -> sut.getCommissions(query, null, from, to)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(commissionRepository);
    }

    @Test
    void getCommissions_withoutPeriod_searchesEveryBusinessDayOfTheSeller() {
        // Arrange
        var query = new PageQuery(1, 20, null, null, null);
        when(commissionRepository.search(eq(SELLER_ID), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        // Act
        var result = sut.getCommissions(query, SELLER_ID, null, null);

        // Assert
        assertThat(result.items()).isEmpty();
        var from = ArgumentCaptor.forClass(LocalDate.class);
        var to = ArgumentCaptor.forClass(LocalDate.class);
        verify(commissionRepository).search(eq(SELLER_ID), from.capture(), to.capture(), any(Pageable.class));
        assertThat(from.getValue()).isBefore(LocalDate.of(2026, 1, 1));
        assertThat(to.getValue()).isAfter(LocalDate.of(2100, 1, 1));
    }

    private static CommissionSource sale() {
        return new CommissionSource(
                CommissionSourceType.SALE,
                SALE_ID,
                "VTE-0042",
                SELLER_ID,
                CHANNEL_ID,
                null,
                LocalDate.of(2026, 11, 8),
                new BigDecimal("25500.00"),
                new BigDecimal("9361.50"));
    }

    private static CommissionTerms terms() {
        return new CommissionTerms(
                new BigDecimal("4.00"), CommissionBase.REVENUE, CommissionRateSource.SALES_CHANNEL, RULE_ID);
    }

    private static UserRDto seller() {
        var role = new RoleRDto(UUID.randomUUID(), RoleName.SELLER, "Vendeuse");
        var now = Instant.now();
        return new UserRDto(
                SELLER_ID,
                "miora@valimbavaka.mg",
                "Miora",
                "Rakoto",
                role,
                CHANNEL_ID,
                new BigDecimal("5.00"),
                true,
                false,
                now,
                now);
    }

    /** The id is set by Hibernate on persist; the mocked repository does it by hand. */
    private static Commission withId(Commission commission) {
        ReflectionTestUtils.setField(commission, "id", COMMISSION_ID);
        return commission;
    }
}
