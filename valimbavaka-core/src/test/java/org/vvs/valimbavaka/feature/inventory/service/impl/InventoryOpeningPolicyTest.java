package org.vvs.valimbavaka.feature.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryScope;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionWDto;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class InventoryOpeningPolicyTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final UUID RANGE_ID = UUID.randomUUID();
    private static final UUID RESPONSIBLE_ID = UUID.randomUUID();
    // 2026-11-07 22:30 UTC = 2026-11-08 01:30 in Antananarivo: the business day is already the 8th.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T22:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    @Mock
    private ReferentialService referentialService;

    @Mock
    private UserService userService;

    @Mock
    private InventorySessionRepository sessionRepository;

    private InventoryOpeningPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryOpeningPolicy(referentialService, userService, sessionRepository, CLOCK);
        lenient().when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(true);
        lenient().when(referentialService.isActiveProductRange(RANGE_ID)).thenReturn(true);
        lenient().when(userService.getUserById(RESPONSIBLE_ID)).thenReturn(user(true));
    }

    @Test
    void check_allScopeToday_returnsOpeningWithResponsibleName() {
        // Act
        var opening = sut.check(request(InventoryScope.ALL, null, TODAY));

        // Assert
        assertThat(opening.locationId()).isEqualTo(LOCATION_ID);
        assertThat(opening.scope()).isEqualTo(InventoryScope.ALL);
        assertThat(opening.rangeId()).isNull();
        assertThat(opening.responsibleName()).isEqualTo("Hanta Rakoto");
        assertThat(opening.countDate()).isEqualTo(TODAY);
    }

    @Test
    void check_rangeScopeWithActiveRange_keepsTheRange() {
        // Act
        var opening = sut.check(request(InventoryScope.RANGE, RANGE_ID, TODAY.plusDays(3)));

        // Assert
        assertThat(opening.rangeId()).isEqualTo(RANGE_ID);
    }

    @Test
    void check_inactiveLocation_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveStockLocation(LOCATION_ID)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(InventoryScope.ALL, null, TODAY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Emplacement");
    }

    @Test
    void check_rangeScopeWithoutRange_throwsBusinessException() {
        assertThatThrownBy(() -> sut.check(request(InventoryScope.RANGE, null, TODAY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("gamme est obligatoire");
    }

    @Test
    void check_allScopeWithRange_throwsBusinessException() {
        assertThatThrownBy(() -> sut.check(request(InventoryScope.ALL, RANGE_ID, TODAY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inventaire complet");
    }

    @Test
    void check_inactiveRange_throwsBusinessException() {
        // Arrange
        when(referentialService.isActiveProductRange(RANGE_ID)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(InventoryScope.RANGE, RANGE_ID, TODAY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Gamme");
    }

    @Test
    void check_countDateYesterday_throwsBusinessException() {
        assertThatThrownBy(() -> sut.check(request(InventoryScope.ALL, null, TODAY.minusDays(1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passé");
    }

    @Test
    void check_unknownResponsible_throwsBusinessException() {
        // Arrange
        when(userService.getUserById(RESPONSIBLE_ID)).thenThrow(new NotFoundException("Utilisateur introuvable"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(InventoryScope.ALL, null, TODAY)))
                .isInstanceOf(BusinessException.class)
                .isNotInstanceOf(NotFoundException.class)
                .hasMessageContaining("Responsable introuvable");
    }

    @Test
    void check_inactiveResponsible_throwsBusinessException() {
        // Arrange
        when(userService.getUserById(RESPONSIBLE_ID)).thenReturn(user(false));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(InventoryScope.ALL, null, TODAY)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void check_sessionAlreadyInProgressInLocation_throwsConflictException() {
        // Arrange
        when(sessionRepository.existsByLocationIdAndStatus(any(UUID.class), any(InventoryStatus.class)))
                .thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(request(InventoryScope.ALL, null, TODAY)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà en cours");
    }

    private static InventorySessionWDto request(InventoryScope scope, @Nullable UUID rangeId, LocalDate countDate) {
        return new InventorySessionWDto(LOCATION_ID, scope, rangeId, RESPONSIBLE_ID, countDate);
    }

    private static UserRDto user(boolean active) {
        var role = new RoleRDto(UUID.randomUUID(), RoleName.SELLER, "Vendeuse");
        var now = Instant.parse("2026-10-01T08:00:00Z");
        return new UserRDto(
                RESPONSIBLE_ID, "hanta@valimbavaka.mg", "Hanta", "Rakoto", role, null, null, active, false, now, now);
    }
}
