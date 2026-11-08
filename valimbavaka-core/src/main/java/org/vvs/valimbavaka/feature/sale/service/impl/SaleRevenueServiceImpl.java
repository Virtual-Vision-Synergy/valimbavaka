package org.vvs.valimbavaka.feature.sale.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.sale.dto.DailySalesRevenueRDto;
import org.vvs.valimbavaka.feature.sale.repository.SaleRevenueRepository;
import org.vvs.valimbavaka.feature.sale.service.SaleRevenueService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@Service
@Transactional(readOnly = true)
public class SaleRevenueServiceImpl implements SaleRevenueService {

    private final SaleRevenueRepository saleRevenueRepository;

    SaleRevenueServiceImpl(SaleRevenueRepository saleRevenueRepository) {
        this.saleRevenueRepository = saleRevenueRepository;
    }

    @Override
    public List<DailySalesRevenueRDto> getDailyRevenue(LocalDate from, LocalDate to) {
        return saleRevenueRepository.sumTotalPerBusinessDate(OperationStatus.VALIDATED, from, to);
    }
}
