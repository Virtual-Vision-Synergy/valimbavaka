package org.vvs.valimbavaka.feature.commission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.mapper.CommissionMapper;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRepository;
import org.vvs.valimbavaka.feature.commission.repository.SellerStatusTotal;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class CommissionFollowUpServiceImplTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID OTHER_SELLER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final LocalDate FIRST = LocalDate.of(2026, 11, 1);
    private static final LocalDate LAST = LocalDate.of(2026, 11, 30);

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private CommissionMapper commissionMapper;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    private CommissionFollowUpServiceImpl sut;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        sut = new CommissionFollowUpServiceImpl(
                commissionRepository, commissionMapper, userService, auditorAware, clock);
    }

    @Test
    void getFollowUp_seller_readsHerOwnFiguresWhateverSellerIdIsRequested() {
        // Arrange : la vendeuse demande les chiffres d'une collègue
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(userService.isActiveSeller(SELLER_ID)).thenReturn(true);
        when(userService.getUserById(SELLER_ID)).thenReturn(user(SELLER_ID, RoleName.SELLER, null));
        when(commissionRepository.totalsBySellerAndStatus(List.of(SELLER_ID), FIRST, LAST))
                .thenReturn(List.of(total(CommissionStatus.CALCULATED, 2, "850.00")));
        when(commissionRepository.sumRevenue(SELLER_ID, FIRST, LAST)).thenReturn(new BigDecimal("17000.00"));

        // Act
        var result = sut.getFollowUp(OTHER_SELLER_ID, NOVEMBER);

        // Assert
        assertThat(result.sellerId()).isEqualTo(SELLER_ID);
        assertThat(result.commissionCount()).isEqualTo(2);
        assertThat(result.commissionAmount()).isEqualByComparingTo("850.00");
        assertThat(result.toValidateAmount()).isEqualByComparingTo("850.00");
        assertThat(result.validatedAmount()).isEqualByComparingTo("0.00");
        verify(userService, never()).getUserById(OTHER_SELLER_ID);
    }

    @Test
    void getFollowUp_targetSet_progressIsRevenueOverTargetRoundedToOneDecimal() {
        // Arrange : 1 000 Ar de CA pour un objectif de 3 000 Ar
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(ADMIN_ID));
        when(userService.isActiveSeller(ADMIN_ID)).thenReturn(false);
        when(userService.getUserById(SELLER_ID))
                .thenReturn(user(SELLER_ID, RoleName.SELLER, new BigDecimal("3000.00")));
        when(commissionRepository.totalsBySellerAndStatus(List.of(SELLER_ID), FIRST, LAST))
                .thenReturn(List.of());
        when(commissionRepository.sumRevenue(SELLER_ID, FIRST, LAST)).thenReturn(new BigDecimal("1000.00"));

        // Act
        var result = sut.getFollowUp(SELLER_ID, NOVEMBER);

        // Assert
        assertThat(result.monthlyTarget()).isEqualByComparingTo("3000.00");
        assertThat(result.targetProgressPercent()).isEqualByComparingTo("33.3");
    }

    @Test
    void getFollowUp_noTargetAndNoSale_hasZeroTotalsAndNullProgress() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());
        when(userService.getUserById(SELLER_ID)).thenReturn(user(SELLER_ID, RoleName.SELLER, null));
        when(commissionRepository.totalsBySellerAndStatus(List.of(SELLER_ID), FIRST, LAST))
                .thenReturn(List.of());
        when(commissionRepository.sumRevenue(SELLER_ID, FIRST, LAST)).thenReturn(null);

        // Act
        var result = sut.getFollowUp(SELLER_ID, NOVEMBER);

        // Assert
        assertThat(result.revenue()).isEqualByComparingTo("0");
        assertThat(result.commissionCount()).isZero();
        assertThat(result.monthlyTarget()).isNull();
        assertThat(result.targetProgressPercent()).isNull();
    }

    @Test
    void getFollowUp_noMonth_usesTheCurrentMonthOfTheClock() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());
        when(userService.getUserById(SELLER_ID)).thenReturn(user(SELLER_ID, RoleName.SELLER, null));
        when(commissionRepository.totalsBySellerAndStatus(List.of(SELLER_ID), FIRST, LAST))
                .thenReturn(List.of());

        // Act
        var result = sut.getFollowUp(SELLER_ID, null);

        // Assert
        assertThat(result.month()).isEqualTo("2026-11");
        assertThat(result.from()).isEqualTo(FIRST);
        assertThat(result.to()).isEqualTo(LAST);
    }

    @Test
    void getFollowUp_administratorWithoutSellerId_throwsBusinessException() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(ADMIN_ID));
        when(userService.isActiveSeller(ADMIN_ID)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.getFollowUp(null, NOVEMBER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vendeuse");
    }

    @Test
    void getFollowUp_administratorNamingANonSeller_throwsNotFound() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(ADMIN_ID));
        when(userService.isActiveSeller(ADMIN_ID)).thenReturn(false);
        when(userService.getUserById(OTHER_SELLER_ID)).thenReturn(user(OTHER_SELLER_ID, RoleName.ADMIN, null));

        // Act + Assert
        assertThatThrownBy(() -> sut.getFollowUp(OTHER_SELLER_ID, NOVEMBER)).isInstanceOf(NotFoundException.class);
    }

    private static SellerStatusTotal total(CommissionStatus status, long count, String amount) {
        return new SellerStatusTotal() {
            @Override
            public UUID getSellerId() {
                return SELLER_ID;
            }

            @Override
            public CommissionStatus getStatus() {
                return status;
            }

            @Override
            public long getCommissionCount() {
                return count;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal(amount);
            }
        };
    }

    private static UserRDto user(UUID id, RoleName roleName, BigDecimal target) {
        var role = new RoleRDto(UUID.randomUUID(), roleName, roleName.name());
        var now = Instant.now();
        return new UserRDto(
                id, "miora@valimbavaka.mg", "Miora", "Rakoto", role, null, null, target, true, false, now, now);
    }
}
