package org.vvs.valimbavaka.feature.shooting.service.impl;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.shooting.domain.Shooting;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingRDto;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingWDto;
import org.vvs.valimbavaka.feature.shooting.mapper.ShootingMapper;
import org.vvs.valimbavaka.feature.shooting.repository.ShootingRepository;
import org.vvs.valimbavaka.feature.shooting.service.ShootingService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class ShootingServiceImpl implements ShootingService {

    private static final Logger log = LoggerFactory.getLogger(ShootingServiceImpl.class);

    static final String RESOURCE_TYPE = "SHOOTING";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "number", "expectedReturnAt", "status");

    private final ShootingRepository shootingRepository;
    private final ShootingMapper shootingMapper;
    private final ShootingPolicy policy;
    private final StockMovementService stockMovementService;
    private final JournalService journalService;

    ShootingServiceImpl(
            ShootingRepository shootingRepository,
            ShootingMapper shootingMapper,
            ShootingPolicy policy,
            StockMovementService stockMovementService,
            JournalService journalService) {
        this.shootingRepository = shootingRepository;
        this.shootingMapper = shootingMapper;
        this.policy = policy;
        this.stockMovementService = stockMovementService;
        this.journalService = journalService;
    }

    @Override
    public PageResponse<ShootingRDto> getShootings(PageQuery query, @Nullable ShootingStatus status) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(
                shootingRepository.search(query.searchTerm(), status, pageable).map(this::toRDto));
    }

    @Override
    public ShootingRDto getShootingById(UUID shootingId) {
        var shooting = shootingRepository
                .findById(shootingId)
                .orElseThrow(() -> new NotFoundException("Sortie shooting introuvable : " + shootingId));
        return toRDto(shooting);
    }

    @Override
    @Transactional
    public ShootingRDto createShooting(ShootingWDto request) {
        var draft = policy.draft(request);
        var number = "SHO-%04d".formatted(shootingRepository.nextNumber());
        var shooting = shootingRepository.save(Shooting.open(number, draft));
        for (var item : shooting.getItems()) {
            stockMovementService.record(new StockMovementCommand(
                    item.getArticleId(),
                    shooting.getLocationId(),
                    StockMovementType.SHOOTING,
                    item.getQuantity().negate(),
                    shooting.getId()));
        }
        journalService.record(
                JournalAction.CREATE,
                RESOURCE_TYPE,
                shooting.getId(),
                "Sortie shooting %s : %d article(s), retour prévu le %s"
                        .formatted(number, shooting.getItems().size(), shooting.getExpectedReturnAt()));
        log.info("Sortie shooting {} enregistrée", shooting.getId());
        return toRDto(shooting);
    }

    private ShootingRDto toRDto(Shooting shooting) {
        return shootingMapper.toRDto(shooting, policy.isLate(shooting));
    }
}
