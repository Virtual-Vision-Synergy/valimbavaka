package org.vvs.valimbavaka.feature.inventory.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryOpening;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryScope;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionWDto;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

/** Rules of an opening (INV-01): active references, scope consistent with the range, date, one session per place. */
@Component
class InventoryOpeningPolicy {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private final ReferentialService referentialService;
    private final UserService userService;
    private final InventorySessionRepository sessionRepository;
    private final Clock clock;

    InventoryOpeningPolicy(
            ReferentialService referentialService,
            UserService userService,
            InventorySessionRepository sessionRepository,
            Clock clock) {
        this.referentialService = referentialService;
        this.userService = userService;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    /** Checks the request and returns the opening to apply; the responsible's name is resolved here. */
    InventoryOpening check(InventorySessionWDto request) {
        if (!referentialService.isActiveStockLocation(request.locationId())) {
            throw new BusinessException("Emplacement introuvable ou inactif : " + request.locationId());
        }
        checkScope(request);
        if (request.countDate().isBefore(LocalDate.now(clock.withZone(BUSINESS_ZONE)))) {
            throw new BusinessException("La date de comptage ne peut pas être dans le passé.");
        }
        var responsible = activeResponsible(request.responsibleId());
        if (sessionRepository.existsByLocationIdAndStatus(request.locationId(), InventoryStatus.IN_PROGRESS)) {
            throw new ConflictException("Une session d'inventaire est déjà en cours dans cet emplacement.");
        }
        return new InventoryOpening(
                request.locationId(),
                request.scope(),
                request.rangeId(),
                responsible.id(),
                responsible.firstName() + " " + responsible.lastName(),
                request.countDate());
    }

    private void checkScope(InventorySessionWDto request) {
        var rangeId = request.rangeId();
        if (request.scope() == InventoryScope.ALL) {
            if (rangeId != null) {
                throw new BusinessException("Aucune gamme ne doit être choisie pour un inventaire complet.");
            }
            return;
        }
        if (rangeId == null) {
            throw new BusinessException("La gamme est obligatoire pour un inventaire par gamme.");
        }
        if (!referentialService.isActiveProductRange(rangeId)) {
            throw new BusinessException("Gamme introuvable ou inactive : " + rangeId);
        }
    }

    private UserRDto activeResponsible(UUID responsibleId) {
        UserRDto user;
        try {
            user = userService.getUserById(responsibleId);
        } catch (NotFoundException e) {
            throw new BusinessException("Responsable introuvable : " + responsibleId);
        }
        if (!user.active()) {
            throw new BusinessException("Le responsable choisi est un compte désactivé.");
        }
        return user;
    }
}
