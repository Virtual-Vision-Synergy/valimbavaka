package org.vvs.valimbavaka.feature.salonservice.service.impl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.commission.service.SalonServiceCommissionRates;
import org.vvs.valimbavaka.feature.salonservice.domain.SalonService;
import org.vvs.valimbavaka.feature.salonservice.repository.SalonServiceRepository;

/**
 * Salon service level of the COM-01 resolver: the rate of the service wins over the channel rule and the seller's
 * rate (PO decision of 07/11/2026, no accumulation). An unknown service or one without a rate gives no rate, and the
 * resolver falls back to the next level. A deactivated service keeps its rate (services already performed).
 */
@Service
@Transactional(readOnly = true)
public class SalonServiceCommissionRatesImpl implements SalonServiceCommissionRates {

    private final SalonServiceRepository serviceRepository;

    public SalonServiceCommissionRatesImpl(SalonServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public Optional<BigDecimal> findRate(UUID salonServiceId) {
        return serviceRepository.findById(salonServiceId).map(SalonService::getCommissionRate);
    }
}
