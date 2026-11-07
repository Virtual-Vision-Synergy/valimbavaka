package org.vvs.valimbavaka.feature.day.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.day.domain.DayTransfer;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningWDto;
import org.vvs.valimbavaka.feature.day.mapper.BusinessDayMapper;
import org.vvs.valimbavaka.feature.day.repository.BusinessDayRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;

@ExtendWith(MockitoExtension.class)
class BusinessDayServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final UUID WAREHOUSE = UUID.randomUUID();
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID DELIVERY = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Mock
    private BusinessDayRepository dayRepository;

    @Mock
    private BusinessDayMapper dayMapper;

    @Mock
    private DayOpeningPolicy policy;

    @Mock
    private DayOpeningGrid grid;

    @Mock
    private TransferService transferService;

    @Mock
    private JournalService journalService;

    private BusinessDayServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new BusinessDayServiceImpl(dayRepository, dayMapper, policy, grid, transferService, journalService);
    }

    @Test
    void openDay_twoDestinations_recordsOneMorningTransferPerDestinationFromTheWarehouse() {
        // Arrange
        var request = new DayOpeningWDto(List.of());
        when(policy.today()).thenReturn(TODAY);
        when(policy.check(TODAY, request)).thenReturn(plan());
        when(transferService.record(any())).thenReturn(transfer("TRF-0093", STORE), transfer("TRF-0094", DELIVERY));
        when(dayRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.openDay(request);

        // Assert
        var commands = ArgumentCaptor.forClass(TransferCommand.class);
        verify(transferService, times(2)).record(commands.capture());
        assertThat(commands.getAllValues()).allSatisfy(command -> {
            assertThat(command.origin()).isEqualTo(TransferOrigin.MORNING);
            assertThat(command.fromLocationId()).isEqualTo(WAREHOUSE);
        });
        assertThat(commands.getAllValues())
                .extracting(TransferCommand::toLocationId)
                .containsExactly(STORE, DELIVERY);
        assertThat(commands.getAllValues().getFirst().lines()).hasSize(2);
        var saved = ArgumentCaptor.forClass(BusinessDay.class);
        verify(dayRepository).saveAndFlush(saved.capture());
        var day = saved.getValue();
        assertThat(day.getBusinessDate()).isEqualTo(TODAY);
        assertThat(day.getStatus()).isEqualTo(DayStatus.OPEN);
        assertThat(day.getTransfers())
                .extracting(DayTransfer::getTransferNumber)
                .containsExactly("TRF-0093", "TRF-0094");
        assertThat(day.getTransfers().getFirst().getLineCount()).isEqualTo(2);
        assertThat(day.getTransfers().getFirst().getQuantity()).isEqualByComparingTo("16");
    }

    @Test
    void openDay_validDispatch_tracesTheOpeningInTheJournal() {
        // Arrange
        var request = new DayOpeningWDto(List.of());
        when(policy.today()).thenReturn(TODAY);
        when(policy.check(TODAY, request)).thenReturn(plan());
        when(transferService.record(any())).thenReturn(transfer("TRF-0093", STORE), transfer("TRF-0094", DELIVERY));
        when(dayRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.openDay(request);

        // Assert
        var expected = "Ouverture de la journée du 08/11/2026 : 2 transfert(s) du matin (TRF-0093, TRF-0094)";
        verify(journalService).record(eq(JournalAction.CREATE), eq("BUSINESS_DAY"), any(), eq(expected));
    }

    @Test
    void requireOpenDay_noDayToday_throwsConflict() {
        // Arrange
        when(policy.today()).thenReturn(TODAY);
        when(dayRepository.findByBusinessDate(TODAY)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.requireOpenDay())
                .isInstanceOf(ConflictException.class)
                .hasMessage("La journée du 08/11/2026 n'est pas ouverte.");
    }

    @Test
    void requireOpenDay_openedToday_returnsTheDay() {
        // Arrange
        var day = BusinessDay.open(TODAY);
        var expected = new BusinessDayRDto(UUID.randomUUID(), TODAY, DayStatus.OPEN, List.of(), Instant.EPOCH, null);
        when(policy.today()).thenReturn(TODAY);
        when(dayRepository.findByBusinessDate(TODAY)).thenReturn(Optional.of(day));
        when(dayMapper.toRDto(day)).thenReturn(expected);

        // Act
        var result = sut.requireOpenDay();

        // Assert
        assertThat(result).isSameAs(expected);
    }

    /** Store: soap 12 and oil 4; delivery: soap 5. */
    private static DispatchPlan plan() {
        var store = new LinkedHashMap<UUID, BigDecimal>();
        store.put(SOAP, new BigDecimal("12"));
        store.put(OIL, new BigDecimal("4"));
        var byDestination = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        byDestination.put(STORE, store);
        byDestination.put(DELIVERY, Map.of(SOAP, new BigDecimal("5")));
        return new DispatchPlan(WAREHOUSE, byDestination);
    }

    private static TransferRDto transfer(String number, UUID destinationId) {
        return new TransferRDto(
                UUID.randomUUID(),
                number,
                TransferOrigin.MORNING,
                TransferStatus.VALIDATED,
                WAREHOUSE,
                destinationId,
                null,
                List.of(),
                Instant.EPOCH,
                null);
    }
}
