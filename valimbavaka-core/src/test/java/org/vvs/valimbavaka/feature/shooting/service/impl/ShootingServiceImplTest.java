package org.vvs.valimbavaka.feature.shooting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.shooting.domain.Shooting;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingDraft;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingItem;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingItemWDto;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingWDto;
import org.vvs.valimbavaka.feature.shooting.mapper.ShootingMapper;
import org.vvs.valimbavaka.feature.shooting.repository.ShootingRepository;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;

@ExtendWith(MockitoExtension.class)
class ShootingServiceImplTest {

    private static final UUID SHOOTING_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final UUID SOAP_ID = UUID.randomUUID();
    private static final UUID SCRUB_ID = UUID.randomUUID();
    private static final Instant EXPECTED_RETURN = Instant.parse("2026-11-08T16:00:00Z");

    @Mock
    private ShootingRepository shootingRepository;

    @Mock
    private ShootingMapper shootingMapper;

    @Mock
    private ShootingPolicy policy;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private JournalService journalService;

    private ShootingServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ShootingServiceImpl(shootingRepository, shootingMapper, policy, stockMovementService, journalService);
    }

    @Test
    void createShooting_validRequest_recordsOneNegativeShootingMovementPerArticleAndJournal() {
        // Arrange
        var request = request();
        when(policy.draft(request)).thenReturn(draft());
        when(shootingRepository.nextNumber()).thenReturn(42L);
        when(shootingRepository.save(any(Shooting.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        // Act
        sut.createShooting(request);

        // Assert
        var commands = ArgumentCaptor.forClass(StockMovementCommand.class);
        verify(stockMovementService, times(2)).record(commands.capture());
        var legs = commands.getAllValues().stream()
                .map(command -> command.articleId() + " " + command.quantity().toPlainString())
                .toList();
        assertThat(legs).containsExactly(SOAP_ID + " -1", SCRUB_ID + " -2.5");
        assertThat(commands.getAllValues()).allSatisfy(command -> {
            assertThat(command.type()).isEqualTo(StockMovementType.SHOOTING);
            assertThat(command.locationId()).isEqualTo(LOCATION_ID);
            assertThat(command.referenceId()).isEqualTo(SHOOTING_ID);
        });
        verify(journalService).record(eq(JournalAction.CREATE), eq("SHOOTING"), eq(SHOOTING_ID), contains("SHO-0042"));
    }

    @Test
    void createShooting_validRequest_returnsMappedExitWithLateFlag() {
        // Arrange
        var request = request();
        when(policy.draft(request)).thenReturn(draft());
        when(shootingRepository.nextNumber()).thenReturn(7L);
        when(shootingRepository.save(any(Shooting.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(policy.isLate(any(Shooting.class))).thenReturn(false);

        // Act
        sut.createShooting(request);

        // Assert
        var mapped = ArgumentCaptor.forClass(Shooting.class);
        verify(shootingMapper).toRDto(mapped.capture(), eq(false));
        assertThat(mapped.getValue().getNumber()).isEqualTo("SHO-0007");
    }

    @Test
    void createShooting_insufficientStock_propagatesConflictWithoutJournal() {
        // Arrange
        var request = request();
        when(policy.draft(request)).thenReturn(draft());
        when(shootingRepository.nextNumber()).thenReturn(1L);
        when(shootingRepository.save(any(Shooting.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(stockMovementService.record(any(StockMovementCommand.class)))
                .thenThrow(new ConflictException("Stock insuffisant à cet emplacement : 0 disponible."));

        // Act + Assert
        assertThatThrownBy(() -> sut.createShooting(request)).isInstanceOf(ConflictException.class);
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void getShootingById_unknownId_throwsNotFoundException() {
        // Arrange
        when(shootingRepository.findById(SHOOTING_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getShootingById(SHOOTING_ID)).isInstanceOf(NotFoundException.class);
    }

    private static ShootingWDto request() {
        return new ShootingWDto(
                LOCATION_ID,
                List.of(
                        new ShootingItemWDto(SOAP_ID, BigDecimal.ONE),
                        new ShootingItemWDto(SCRUB_ID, new BigDecimal("2.5"))),
                EXPECTED_RETURN,
                null);
    }

    private static ShootingDraft draft() {
        var soap = ShootingItem.of(SOAP_ID, "ART-0001", "Savon noir 100 g", BigDecimal.ONE);
        var scrub = ShootingItem.of(SCRUB_ID, "ART-0031", "Gommage café 200 g", new BigDecimal("2.5"));
        return new ShootingDraft(LOCATION_ID, "Miora Andria", EXPECTED_RETURN, null, List.of(soap, scrub));
    }

    /** The id is set by Hibernate on persist; the mocked repository does it by hand. */
    private static Shooting withId(Shooting shooting) {
        ReflectionTestUtils.setField(shooting, "id", SHOOTING_ID);
        return shooting;
    }
}
