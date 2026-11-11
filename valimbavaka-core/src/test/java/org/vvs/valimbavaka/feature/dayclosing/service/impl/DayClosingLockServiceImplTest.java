package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
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
import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayclosing.domain.ClosingGapReason;
import org.vvs.valimbavaka.feature.dayclosing.domain.CountResult;
import org.vvs.valimbavaka.feature.dayclosing.domain.DayClosingCount;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLocationRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.repository.DayClosingCountRepository;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

@ExtendWith(MockitoExtension.class)
class DayClosingLockServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-11T18:00:00Z");
    private static final Instant EARLIER = NOW.minusSeconds(3600);
    private static final UUID DAY_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID SOAP_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();

    @Mock
    private BusinessDayService businessDayService;

    @Mock
    private DayClosingService dayClosingService;

    @Mock
    private DayClosingCountRepository countRepository;

    @Mock
    private JournalService journalService;

    private DayClosingLockServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DayClosingLockServiceImpl(
                businessDayService,
                dayClosingService,
                countRepository,
                journalService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void lock_everyLineCountedAndJustified_closesTheDayAndWritesOneJournalLine() {
        // Arrange: soap 10 theoretical, 8 counted with a reason; oil 5 theoretical, 5 counted
        stubClosing(line(SOAP_ID, "ART-0031", "10"), line(OIL_ID, "ART-0032", "5"));
        stubSaved(saved(SOAP_ID, "10", "8", ClosingGapReason.LOSS), saved(OIL_ID, "5", "5", null));

        // Act
        sut.lock();

        // Assert
        var order = inOrder(businessDayService, journalService);
        order.verify(businessDayService).lockOpenDay();
        order.verify(businessDayService).closeDay();
        var description = ArgumentCaptor.forClass(String.class);
        order.verify(journalService)
                .record(any(JournalAction.class), any(String.class), any(UUID.class), description.capture());
        var expected = "Clôture de la journée du 11/11/2026 : 2 ligne(s) comptée(s), 1 écart(s) (ART-0031 -2 LOSS)";
        assertThat(description.getValue()).isEqualTo(expected);
    }

    @Test
    void lock_lineNotCounted_throwsBusinessExceptionAndClosesNothing() {
        // Arrange: oil has no count
        stubClosing(line(SOAP_ID, "ART-0031", "10"), line(OIL_ID, "ART-0032", "5"));
        stubSaved(saved(SOAP_ID, "10", "10", null));

        // Act + Assert
        assertThatThrownBy(() -> sut.lock())
                .isInstanceOf(BusinessException.class)
                .hasMessage("Toutes les lignes doivent être comptées avant le verrouillage : ART-0032.");
        verify(businessDayService, never()).closeDay();
        verify(countRepository, never()).saveAll(anyList());
        verifyNoInteractions(journalService);
    }

    @Test
    void lock_saleAfterTheCount_theNewGapWithoutReasonRefusesTheLock() {
        // Arrange: counted 10 when 10 was expected (no reason); a sale of 2 since then makes the theoretical 8
        stubClosing(line(SOAP_ID, "ART-0031", "8"));
        stubSaved(saved(SOAP_ID, "10", "10", null));

        // Act + Assert
        assertThatThrownBy(() -> sut.lock())
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un motif est obligatoire pour chaque écart : ART-0031.");
        verify(businessDayService, never()).closeDay();
        verifyNoInteractions(journalService);
    }

    @Test
    void lock_saleAfterTheCountWithTheReasonAlreadyGiven_freezesTheNewTheoretical() {
        // Arrange: counted 8 with a reason when 10 was expected; a sale of 1 makes the theoretical 9
        stubClosing(line(SOAP_ID, "ART-0031", "9"));
        var count = saved(SOAP_ID, "10", "8", ClosingGapReason.BREAKAGE);
        stubSaved(count);

        // Act
        sut.lock();

        // Assert
        assertThat(count.getTheoreticalQuantity()).isEqualByComparingTo("9");
        assertThat(count.getReason()).isEqualTo(ClosingGapReason.BREAKAGE);
        assertThat(count.getCountedAt()).isEqualTo(NOW);
        verify(countRepository).saveAll(List.of(count));
    }

    @Test
    void lock_saleAfterTheCountRemovesTheGap_clearsTheReason() {
        // Arrange: counted 8 (reason LOSS) against 10; two units were sold since: the theoretical is 8, no gap
        stubClosing(line(SOAP_ID, "ART-0031", "8"));
        var count = saved(SOAP_ID, "10", "8", ClosingGapReason.LOSS);
        stubSaved(count);

        // Act
        sut.lock();

        // Assert
        assertThat(count.getReason()).isNull();
        var description = ArgumentCaptor.forClass(String.class);
        verify(journalService)
                .record(any(JournalAction.class), any(String.class), any(UUID.class), description.capture());
        assertThat(description.getValue()).endsWith("1 ligne(s) comptée(s), 0 écart(s)");
    }

    @Test
    void lock_countsAlreadyUpToDate_savesNothingNew() {
        // Arrange
        stubClosing(line(SOAP_ID, "ART-0031", "10"));
        stubSaved(saved(SOAP_ID, "10", "10", null));

        // Act
        sut.lock();

        // Assert
        verify(countRepository).saveAll(List.of());
        verify(businessDayService).closeDay();
    }

    @Test
    void lock_moreThanTenNotCounted_listsTenAndCountsTheOthers() {
        // Arrange
        var lines = new ArrayList<ClosingLineRDto>();
        for (var i = 1; i <= 12; i++) {
            lines.add(line(UUID.randomUUID(), "ART-%04d".formatted(i), "1"));
        }
        stubClosing(lines.toArray(ClosingLineRDto[]::new));
        stubSaved();

        // Act + Assert
        assertThatThrownBy(() -> sut.lock())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ART-0010 et 2 autre(s).");
    }

    @Test
    void lock_dayNotOpen_propagatesTheConflictAndReadsNothing() {
        // Arrange
        var closed = new ConflictException("La journée du 11/11/2026 est déjà clôturée.");
        when(businessDayService.lockOpenDay()).thenThrow(closed);

        // Act + Assert
        assertThatThrownBy(() -> sut.lock()).isInstanceOf(ConflictException.class);
        verifyNoInteractions(dayClosingService, countRepository, journalService);
    }

    @Test
    void lock_emptyReconciliation_closesTheDay() {
        // Arrange
        stubClosing();
        stubSaved();

        // Act
        sut.lock();

        // Assert
        verify(businessDayService).closeDay();
    }

    private void stubClosing(ClosingLineRDto... lines) {
        var store = new ClosingLocationRDto(STORE_ID, "STORE", "Boutique", List.of(lines));
        var closing = new DayClosingRDto(DAY_ID, LocalDate.of(2026, 11, 11), DayStatus.OPEN, List.of(store));
        when(dayClosingService.getClosing()).thenReturn(closing);
    }

    private void stubSaved(DayClosingCount... counts) {
        when(countRepository.findByBusinessDayId(DAY_ID)).thenReturn(List.of(counts));
    }

    private static DayClosingCount saved(UUID articleId, String theoretical, String counted, ClosingGapReason reason) {
        var result = new CountResult(new BigDecimal(counted), new BigDecimal(theoretical), reason, EARLIER);
        return DayClosingCount.create(DAY_ID, STORE_ID, articleId, result);
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
}
