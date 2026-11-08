package org.vvs.valimbavaka.feature.commission.service.impl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRule;
import org.vvs.valimbavaka.feature.commission.dto.ApplicableCommissionRDto;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRuleRepository;
import org.vvs.valimbavaka.feature.commission.service.CommissionRateResolver;
import org.vvs.valimbavaka.feature.commission.service.SalonServiceCommissionRates;
import org.vvs.valimbavaka.feature.user.service.UserService;

/**
 * Most precise level first, no accumulation (PO decision of 07/11/2026): salon service, then the active rule of the
 * channel, then the seller's rate. A salon service rate and a seller rate apply to the revenue; a channel rule carries
 * its own base.
 */
@Service
@Transactional(readOnly = true)
public class CommissionRateResolverImpl implements CommissionRateResolver {

    private static final BigDecimal NO_RATE = new BigDecimal("0.00");

    private final CommissionRuleRepository ruleRepository;
    private final UserService userService;
    private final ObjectProvider<SalonServiceCommissionRates> salonServiceRates;

    public CommissionRateResolverImpl(
            CommissionRuleRepository ruleRepository,
            UserService userService,
            ObjectProvider<SalonServiceCommissionRates> salonServiceRates) {
        this.ruleRepository = ruleRepository;
        this.userService = userService;
        this.salonServiceRates = salonServiceRates;
    }

    @Override
    public ApplicableCommissionRDto resolve(UUID sellerId, UUID salesChannelId, @Nullable UUID salonServiceId) {
        var salonRate = salonServiceRate(salonServiceId);
        if (salonRate.isPresent()) {
            return applicable(salonRate.get(), CommissionRateSource.SALON_SERVICE);
        }
        var rule = ruleRepository.findBySalesChannelIdAndActiveTrue(salesChannelId);
        if (rule.isPresent()) {
            return channelRule(rule.get());
        }
        var sellerRate = userService.getUserById(sellerId).commissionRate();
        if (sellerRate != null) {
            return applicable(sellerRate, CommissionRateSource.SELLER);
        }
        return applicable(NO_RATE, CommissionRateSource.NONE);
    }

    /** Empty when no service is sold or when no feature provides salon service rates yet (before SRV-01). */
    private Optional<BigDecimal> salonServiceRate(@Nullable UUID salonServiceId) {
        if (salonServiceId == null) {
            return Optional.empty();
        }
        var rates = salonServiceRates.getIfAvailable();
        return rates == null ? Optional.empty() : rates.findRate(salonServiceId);
    }

    private static ApplicableCommissionRDto channelRule(CommissionRule rule) {
        var source = CommissionRateSource.SALES_CHANNEL;
        return new ApplicableCommissionRDto(rule.getRate(), rule.getBase(), source, rule.getId());
    }

    private static ApplicableCommissionRDto applicable(BigDecimal rate, CommissionRateSource source) {
        return new ApplicableCommissionRDto(rate, CommissionBase.REVENUE, source, null);
    }
}
