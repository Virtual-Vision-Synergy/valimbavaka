package org.vvs.valimbavaka.feature.salonservice.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecord;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonConsumptionRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordWDto;
import org.vvs.valimbavaka.feature.salonservice.mapper.ServiceRecordMapper;
import org.vvs.valimbavaka.feature.salonservice.repository.ServiceRecordRepository;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class ServiceRecordServiceImpl implements ServiceRecordService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRecordServiceImpl.class);

    static final String RESOURCE_TYPE = "SERVICE_RECORD";

    /** Stock movement of a salon consumption: the « salon » type of STK-01 (see the SRV-02 guide). */
    static final StockMovementType SALON_MOVEMENT = StockMovementType.TRADE_SHOW;

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "number", "serviceName", "price");

    private final ServiceRecordRepository serviceRecordRepository;
    private final ServiceRecordMapper serviceRecordMapper;
    private final ServiceRecordPolicy policy;
    private final StockMovementService stockMovementService;
    private final JournalService journalService;

    ServiceRecordServiceImpl(
            ServiceRecordRepository serviceRecordRepository,
            ServiceRecordMapper serviceRecordMapper,
            ServiceRecordPolicy policy,
            StockMovementService stockMovementService,
            JournalService journalService) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.serviceRecordMapper = serviceRecordMapper;
        this.policy = policy;
        this.stockMovementService = stockMovementService;
        this.journalService = journalService;
    }

    @Override
    public PageResponse<ServiceRecordRDto> getServiceRecords(PageQuery query) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(
                serviceRecordRepository.search(query.searchTerm(), pageable).map(serviceRecordMapper::toRDto));
    }

    @Override
    public ServiceRecordRDto getServiceRecordById(UUID serviceRecordId) {
        return serviceRecordRepository
                .findById(serviceRecordId)
                .map(serviceRecordMapper::toRDto)
                .orElseThrow(() -> new NotFoundException("Prestation introuvable : " + serviceRecordId));
    }

    @Override
    @Transactional
    public ServiceRecordRDto recordService(ServiceRecordWDto request) {
        var draft = policy.draft(request);
        var number = "PRE-%04d".formatted(serviceRecordRepository.nextNumber());
        var serviceRecord = serviceRecordRepository.save(ServiceRecord.record(number, draft));
        for (var product : serviceRecord.getProducts()) {
            stockMovementService.record(new StockMovementCommand(
                    product.getArticleId(),
                    serviceRecord.getLocationId(),
                    SALON_MOVEMENT,
                    product.getQuantity().negate(),
                    serviceRecord.getId()));
        }
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                serviceRecord.getId(),
                "Prestation %s : %s (%s Ar), %d produit(s) consommé(s) au salon"
                        .formatted(
                                number,
                                serviceRecord.getServiceName(),
                                serviceRecord.getPrice().toPlainString(),
                                serviceRecord.getProducts().size()));
        log.info("Prestation {} enregistrée", serviceRecord.getId());
        return serviceRecordMapper.toRDto(serviceRecord);
    }

    @Override
    public List<ServiceRecordRDto> getServiceRecordsBetween(Instant from, Instant to) {
        return serviceRecordRepository.findPerformedBetween(from, to).stream()
                .map(serviceRecordMapper::toRDto)
                .toList();
    }

    @Override
    public List<SalonConsumptionRDto> getSalonConsumption(Instant from, Instant to) {
        return serviceRecordRepository.consumptionBetween(from, to);
    }
}
