package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.dayclosing.domain.ClosingGapReason;
import org.vvs.valimbavaka.feature.dayclosing.domain.CountResult;
import org.vvs.valimbavaka.feature.dayclosing.domain.DayClosingCount;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingCountWDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLocationRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingCountsWDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.repository.DayClosingCountRepository;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

@ExtendWith(MockitoExtension.class)
class DayClosingCountServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
    private static final Instant EARLIER = NOW.minusSeconds(600);
    private static final UUID DAY_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID SOAP_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();

    @Mock
    private DayClosingService dayClosingService;

    @Mock
    private DayClosingCountRepository countRepository;

    @Mock
    private JournalService journalService;

    private DayClosingCountServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DayClosingCountServiceImpl(
                dayClosingService, countRepository, journalService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void saveCounts_gapWithoutReason_throwsBusinessExceptionNamingTheArticleAndSavesNothing() {
        // Arrange: 10 expected, 8 counted, no reason
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"));

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(request(count(SOAP_ID, "8", null))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un motif est obligatoire pour chaque écart : ART-0031.");
        verify(countRepository, never()).saveAll(anyList());
        verifyNoInteractions(journalService);
    }

    @Test
    void saveCounts_oneGapWithoutReasonAmongValidLines_refusesTheWholeSave() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"), line(OIL_ID, "ART-0032", "5"));
        var request = request(count(SOAP_ID, "10", null), count(OIL_ID, "4", null));

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un motif est obligatoire pour chaque écart : ART-0032.");
        verify(countRepository, never()).saveAll(anyList());
    }

    @Test
    void saveCounts_moreThanTenGapsWithoutReason_listsTenAndCountsTheOthers() {
        // Arrange
        var lines = new ArrayList<ClosingLineRDto>();
        var counts = new ArrayList<ClosingCountWDto>();
        for (var i = 1; i <= 12; i++) {
            var id = UUID.randomUUID();
            lines.add(line(id, "ART-%04d".formatted(i), "10"));
            counts.add(count(id, "9", null));
        }
        stubClosing(DayStatus.OPEN, lines.toArray(ClosingLineRDto[]::new));

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(new DayClosingCountsWDto(counts)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ART-0010 et 2 autre(s)");
    }

    @Test
    void saveCounts_gapWithReason_savesTheCountWithTheTheoreticalFrozenAndWritesTheJournal() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"));
        stubNoSavedCount();

        // Act
        sut.saveCounts(request(count(SOAP_ID, "8", ClosingGapReason.BREAKAGE)));

        // Assert
        var saved = savedCounts().getFirst();
        assertThat(saved.getBusinessDayId()).isEqualTo(DAY_ID);
        assertThat(saved.getLocationId()).isEqualTo(STORE_ID);
        assertThat(saved.getArticleId()).isEqualTo(SOAP_ID);
        assertThat(saved.getCountedQuantity()).isEqualByComparingTo("8");
        assertThat(saved.getTheoreticalQuantity()).isEqualByComparingTo("10");
        assertThat(saved.getReason()).isEqualTo(ClosingGapReason.BREAKAGE);
        assertThat(saved.getCountedAt()).isEqualTo(NOW);
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "BUSINESS_DAY",
                        DAY_ID,
                        "Comptage de clôture du 11/11/2026 : 1 ligne(s) enregistrée(s), dont 1 écart(s) motivé(s)");
    }

    @Test
    void saveCounts_countEqualToTheTheoretical_needsNoReasonAndIgnoresOneSent() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"), line(OIL_ID, "ART-0032", "5"));
        stubNoSavedCount();

        // Act
        sut.saveCounts(request(count(SOAP_ID, "10.000", null), count(OIL_ID, "5", ClosingGapReason.LOSS)));

        // Assert
        assertThat(savedCounts()).hasSize(2).allSatisfy(count -> {
            assertThat(count.hasGap()).isFalse();
            assertThat(count.getReason()).isNull();
        });
    }

    @Test
    void saveCounts_zeroCountedWhereStockWasExpected_isAGapThatNeedsAReason() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "3"));

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(request(count(SOAP_ID, "0", null))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void saveCounts_recountOfASavedLine_replacesTheCountAndClearsTheReasonWithoutGap() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"));
        var before = new CountResult(new BigDecimal("8"), new BigDecimal("10"), ClosingGapReason.LOSS, EARLIER);
        var previous = DayClosingCount.create(DAY_ID, STORE_ID, SOAP_ID, before);
        when(countRepository.findByBusinessDayId(DAY_ID)).thenReturn(List.of(previous));
        when(countRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.saveCounts(request(count(SOAP_ID, "10", null)));

        // Assert
        assertThat(savedCounts()).containsExactly(previous);
        assertThat(previous.getCountedQuantity()).isEqualByComparingTo("10");
        assertThat(previous.getReason()).isNull();
        assertThat(previous.getCountedAt()).isEqualTo(NOW);
    }

    @Test
    void saveCounts_sameCountSentAgain_savesNothingAndWritesNoJournalLine() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"));
        var before = new CountResult(new BigDecimal("8"), new BigDecimal("10"), ClosingGapReason.LOSS, EARLIER);
        var previous = DayClosingCount.create(DAY_ID, STORE_ID, SOAP_ID, before);
        when(countRepository.findByBusinessDayId(DAY_ID)).thenReturn(List.of(previous));
        when(countRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.saveCounts(request(count(SOAP_ID, "8.0", ClosingGapReason.LOSS)));

        // Assert
        assertThat(savedCounts()).isEmpty();
        assertThat(previous.getCountedAt()).isEqualTo(EARLIER);
        verifyNoInteractions(journalService);
    }

    @Test
    void saveCounts_lineNotInTodaysClosing_throwsNotFound() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"));

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(request(count(UUID.randomUUID(), "1", null))))
                .isInstanceOf(NotFoundException.class);
        verify(countRepository, never()).saveAll(anyList());
    }

    @Test
    void saveCounts_sameLineTwice_throwsBusinessException() {
        // Arrange
        stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"));
        var request = request(count(SOAP_ID, "10", null), count(SOAP_ID, "9", null));

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un article ne peut être compté qu'une fois par emplacement : ART-0031.");
    }

    @Test
    void saveCounts_closedDay_throwsConflict() {
        // Arrange
        stubClosing(DayStatus.CLOSED, line(SOAP_ID, "ART-0031", "10"));

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(request(count(SOAP_ID, "10", null))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("La journée du 11/11/2026 est déjà clôturée.");
        verify(countRepository, never()).saveAll(anyList());
    }

    @Test
    void saveCounts_dayNotOpened_propagatesTheConflictOfTheReconciliation() {
        // Arrange
        var notOpened = new ConflictException("La journée du 11/11/2026 n'est pas ouverte.");
        when(dayClosingService.getClosing()).thenThrow(notOpened);

        // Act + Assert
        assertThatThrownBy(() -> sut.saveCounts(request(count(SOAP_ID, "10", null))))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(countRepository, journalService);
    }

    @Test
    void saveCounts_saved_returnsTheReconciliationReadAfterTheSave() {
        // Arrange
        var closing = stubClosing(DayStatus.OPEN, line(SOAP_ID, "ART-0031", "10"));
        stubNoSavedCount();

        // Act
        var result = sut.saveCounts(request(count(SOAP_ID, "10", null)));

        // Assert
        assertThat(result).isSameAs(closing);
        verify(journalService).record(eq(JournalAction.UPDATE), anyString(), any(UUID.class), anyString());
    }

    private void stubNoSavedCount() {
        when(countRepository.findByBusinessDayId(DAY_ID)).thenReturn(List.of());
        when(countRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private List<DayClosingCount> savedCounts() {
        var captor = ArgumentCaptor.forClass(List.class);
        verify(countRepository).saveAll(captor.capture());
        return (List<DayClosingCount>) captor.getValue();
    }

    private DayClosingRDto stubClosing(DayStatus status, ClosingLineRDto... lines) {
        var store = new ClosingLocationRDto(STORE_ID, "STORE", "Boutique", List.of(lines));
        var closing = new DayClosingRDto(DAY_ID, LocalDate.of(2026, 11, 11), status, List.of(store));
        when(dayClosingService.getClosing()).thenReturn(closing);
        return closing;
    }

    private static ClosingLineRDto line(UUID articleId, String reference, String theoretical) {
        var quantity = new BigDecimal(theoretical);
        return new ClosingLineRDto(
                articleId,
                reference,
                "Article " + reference,
                BigDecimal.ZERO,
                quantity,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                quantity,
                quantity,
                BigDecimal.ZERO,
                null,
                null);
    }

    private static ClosingCountWDto count(UUID articleId, String counted, ClosingGapReason reason) {
        return new ClosingCountWDto(STORE_ID, articleId, new BigDecimal(counted), reason);
    }

    private static DayClosingCountsWDto request(ClosingCountWDto... counts) {
        return new DayClosingCountsWDto(List.of(counts));
    }
}
