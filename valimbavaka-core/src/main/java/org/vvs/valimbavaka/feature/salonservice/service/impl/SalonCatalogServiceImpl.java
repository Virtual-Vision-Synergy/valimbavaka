package org.vvs.valimbavaka.feature.salonservice.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.salonservice.domain.ConsumedProduct;
import org.vvs.valimbavaka.feature.salonservice.domain.SalonService;
import org.vvs.valimbavaka.feature.salonservice.domain.SalonServiceDetails;
import org.vvs.valimbavaka.feature.salonservice.dto.ConsumedProductWDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceWDto;
import org.vvs.valimbavaka.feature.salonservice.mapper.SalonServiceMapper;
import org.vvs.valimbavaka.feature.salonservice.repository.SalonServiceRepository;
import org.vvs.valimbavaka.feature.salonservice.service.SalonCatalogService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class SalonCatalogServiceImpl implements SalonCatalogService {

    private static final Logger log = LoggerFactory.getLogger(SalonCatalogServiceImpl.class);

    private static final String RESOURCE_TYPE = "SALON_SERVICE";
    private static final String NAME_ALREADY_USED = "Un service salon avec ce nom existe déjà.";
    private static final String STALE_VERSION =
            "Le service salon a été modifié entre-temps : rechargez la fiche puis réessayez.";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "price", "durationMinutes");

    private final SalonServiceRepository serviceRepository;
    private final SalonServiceMapper serviceMapper;
    private final ConsumedProductChecker productChecker;
    private final JournalService journalService;

    public SalonCatalogServiceImpl(
            SalonServiceRepository serviceRepository,
            SalonServiceMapper serviceMapper,
            ConsumedProductChecker productChecker,
            JournalService journalService) {
        this.serviceRepository = serviceRepository;
        this.serviceMapper = serviceMapper;
        this.productChecker = productChecker;
        this.journalService = journalService;
    }

    @Override
    public PageResponse<SalonServiceRDto> getServices(PageQuery query, @Nullable Boolean active) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(
                serviceRepository.search(query.searchTerm(), active, pageable).map(serviceMapper::toRDto));
    }

    @Override
    public SalonServiceRDto getServiceById(UUID serviceId) {
        return serviceMapper.toRDto(findService(serviceId));
    }

    @Override
    @Transactional
    public SalonServiceRDto createService(SalonServiceWDto request) {
        var details = toDetails(request);
        productChecker.check(details.products(), Set.of());
        if (serviceRepository.existsByNameIgnoreCase(details.name().strip())) {
            throw new ConflictException(NAME_ALREADY_USED);
        }
        SalonService saved = serviceRepository.save(SalonService.create(details));
        String description = "Création du service salon " + label(saved);
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, saved.getId(), description);
        log.info("Service salon {} créé", saved.getId());
        return serviceMapper.toRDto(saved);
    }

    @Override
    @Transactional
    public SalonServiceRDto updateService(UUID serviceId, SalonServiceWDto request) {
        SalonService service = findService(serviceId);
        if (request.version() != null && request.version() != service.getVersion()) {
            throw new ConflictException(STALE_VERSION);
        }
        var details = toDetails(request);
        productChecker.check(details.products(), listedArticles(service));
        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(details.name().strip(), serviceId)) {
            throw new ConflictException(NAME_ALREADY_USED);
        }
        if (service.update(details)) {
            String description = "Modification du service salon " + label(service);
            journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, serviceId, description);
        }
        return serviceMapper.toRDto(service);
    }

    @Override
    @Transactional
    public SalonServiceRDto setActive(UUID serviceId, boolean active) {
        SalonService service = findService(serviceId);
        if (service.isActive() == active) {
            return serviceMapper.toRDto(service);
        }
        if (active) {
            service.activate();
        } else {
            service.deactivate();
        }
        String action = active ? "Activation" : "Désactivation";
        String description = action + " du service salon " + label(service);
        journalService.record(JournalAction.UPDATE, RESOURCE_TYPE, serviceId, description);
        log.info("Service salon {} {}", serviceId, active ? "activé" : "désactivé");
        return serviceMapper.toRDto(service);
    }

    private SalonService findService(UUID serviceId) {
        return serviceRepository
                .findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service salon introuvable : " + serviceId));
    }

    private static Set<UUID> listedArticles(SalonService service) {
        return service.getProducts().stream().map(ConsumedProduct::getArticleId).collect(Collectors.toSet());
    }

    private static String label(SalonService service) {
        return service.getName() + " (" + service.getPrice().toPlainString() + " Ar)";
    }

    private static SalonServiceDetails toDetails(SalonServiceWDto request) {
        var lines = request.products() == null ? List.<ConsumedProductWDto>of() : request.products();
        var products = lines.stream()
                .filter(Objects::nonNull)
                .map(line -> new ConsumedProduct(line.articleId(), line.quantity()))
                .toList();
        return new SalonServiceDetails(
                request.name(), request.durationMinutes(), request.price(), request.commissionRate(), products);
    }
}
