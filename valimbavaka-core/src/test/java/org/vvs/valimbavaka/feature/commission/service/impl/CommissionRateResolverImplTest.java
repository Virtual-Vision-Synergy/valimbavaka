package org.vvs.valimbavaka.feature.commission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRule;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRuleDetails;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRuleRepository;
import org.vvs.valimbavaka.feature.commission.service.SalonServiceCommissionRates;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

/** PO decision of 07/11/2026: salon service rate, otherwise channel rule, otherwise seller rate; never added up. */
@ExtendWith(MockitoExtension.class)
class CommissionRateResolverImplTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID SHOP_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();

    @Mock
    private CommissionRuleRepository ruleRepository;

    @Mock
    private UserService userService;

    @Mock
    private ObjectProvider<SalonServiceCommissionRates> salonServiceRatesProvider;

    @Mock
    private SalonServiceCommissionRates salonServiceRates;

    private CommissionRateResolverImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CommissionRateResolverImpl(ruleRepository, userService, salonServiceRatesProvider);
    }

    @Test
    void resolve_salonServiceWithRate_returnsServiceRateWithoutReadingChannelOrSeller() {
        // Arrange
        when(salonServiceRatesProvider.getIfAvailable()).thenReturn(salonServiceRates);
        when(salonServiceRates.findRate(SERVICE_ID)).thenReturn(Optional.of(new BigDecimal("10.00")));

        // Act
        var result = sut.resolve(SELLER_ID, SHOP_ID, SERVICE_ID);

        // Assert
        assertThat(result.rate()).isEqualByComparingTo("10.00");
        assertThat(result.source()).isEqualTo(CommissionRateSource.SALON_SERVICE);
        assertThat(result.base()).isEqualTo(CommissionBase.REVENUE);
        assertThat(result.ruleId()).isNull();
        verifyNoInteractions(ruleRepository, userService);
    }

    @Test
    void resolve_salonServiceWithoutProvider_fallsBackToChannelRule() {
        // Arrange
        when(salonServiceRatesProvider.getIfAvailable()).thenReturn(null);
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.of(shopRule()));

        // Act
        var result = sut.resolve(SELLER_ID, SHOP_ID, SERVICE_ID);

        // Assert
        assertThat(result.rate()).isEqualByComparingTo("3.00");
        assertThat(result.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        verifyNoInteractions(userService);
    }

    @Test
    void resolve_salonServiceWithoutRate_fallsBackToChannelRule() {
        // Arrange
        when(salonServiceRatesProvider.getIfAvailable()).thenReturn(salonServiceRates);
        when(salonServiceRates.findRate(SERVICE_ID)).thenReturn(Optional.empty());
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.of(shopRule()));

        // Act
        var result = sut.resolve(SELLER_ID, SHOP_ID, SERVICE_ID);

        // Assert
        assertThat(result.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
    }

    @Test
    void resolve_activeChannelRule_winsOverSellerRateWithTheRuleBase() {
        // Arrange
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.of(shopRule()));

        // Act
        var result = sut.resolve(SELLER_ID, SHOP_ID, null);

        // Assert
        assertThat(result.rate()).isEqualByComparingTo("3.00");
        assertThat(result.base()).isEqualTo(CommissionBase.MARGIN);
        assertThat(result.source()).isEqualTo(CommissionRateSource.SALES_CHANNEL);
        verifyNoInteractions(userService, salonServiceRatesProvider);
    }

    @Test
    void resolve_noChannelRule_returnsSellerRate() {
        // Arrange
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.empty());
        when(userService.getUserById(SELLER_ID)).thenReturn(user(new BigDecimal("5.00")));

        // Act
        var result = sut.resolve(SELLER_ID, SHOP_ID, null);

        // Assert
        assertThat(result.rate()).isEqualByComparingTo("5.00");
        assertThat(result.base()).isEqualTo(CommissionBase.REVENUE);
        assertThat(result.source()).isEqualTo(CommissionRateSource.SELLER);
        assertThat(result.ruleId()).isNull();
    }

    @Test
    void resolve_noLevelGivesARate_returnsZeroWithSourceNone() {
        // Arrange
        when(ruleRepository.findBySalesChannelIdAndActiveTrue(SHOP_ID)).thenReturn(Optional.empty());
        when(userService.getUserById(SELLER_ID)).thenReturn(user(null));

        // Act
        var result = sut.resolve(SELLER_ID, SHOP_ID, null);

        // Assert
        assertThat(result.rate()).isEqualByComparingTo("0");
        assertThat(result.source()).isEqualTo(CommissionRateSource.NONE);
    }

    private static CommissionRule shopRule() {
        return CommissionRule.create(
                new CommissionRuleDetails("Boutique marge", SHOP_ID, new BigDecimal("3"), CommissionBase.MARGIN));
    }

    private static UserRDto user(BigDecimal commissionRate) {
        var role = new RoleRDto(UUID.randomUUID(), RoleName.SELLER, "Vendeuse");
        var now = Instant.now();
        return new UserRDto(
                SELLER_ID,
                "miora@valimbavaka.mg",
                "Miora",
                "Andriamasy",
                role,
                SHOP_ID,
                commissionRate,
                true,
                false,
                now,
                now);
    }
}
