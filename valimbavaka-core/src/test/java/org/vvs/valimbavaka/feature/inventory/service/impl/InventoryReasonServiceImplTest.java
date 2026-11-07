package org.vvs.valimbavaka.feature.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryArticle;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryReason;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryReasonWDto;
import org.vvs.valimbavaka.feature.inventory.mapper.InventoryMapper;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;

@ExtendWith(MockitoExtension.class)
class InventoryReasonServiceImplTest {

    private static final Instant COUNTED_AT = Instant.parse("2026-11-08T07:15:00Z");
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID LINE_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();

    @Mock
    private InventorySessionRepository sessionRepository;

    @Mock
    private InventoryLineRepository lineRepository;

    @Mock
    private ArticleService articleService;

    @Mock
    private JournalService journalService;

    private InventorySession session;
    private InventoryLine line;
    private InventoryReasonServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryReasonServiceImpl(
                sessionRepository, lineRepository, new InventoryMapper(), articleService, journalService);
        session = InventorySession.open("INV-0012", InventoryTestData.opening(null));
        line = InventoryLine.toCount(
                SESSION_ID, new InventoryArticle(ARTICLE_ID, "ART-0031", "Savon noir 100 g", "3210000000031"));
    }

    @Test
    void qualifyLine_lineWithNegativeGap_recordsReasonAndJournals() {
        // Arrange
        givenSessionAndLine();
        line.count(new BigDecimal("10"), new BigDecimal("12"), COUNTED_AT);
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of(ARTICLE_ID, new BigDecimal("3120.50")));

        // Act
        var result = sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.BREAKAGE));

        // Assert
        assertThat(result.reason()).isEqualTo(InventoryReason.BREAKAGE);
        assertThat(result.theoreticalQuantity()).isEqualByComparingTo("12");
        assertThat(result.gap()).isEqualByComparingTo("-2");
        assertThat(result.unitCost()).isEqualByComparingTo("3120.50");
        assertThat(line.getReason()).isEqualTo(InventoryReason.BREAKAGE);
        verify(lineRepository).save(line);
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "INVENTORY_SESSION",
                        SESSION_ID,
                        "Motif d'écart de ART-0031 dans INV-0012 : Casse");
    }

    @Test
    void qualifyLine_lineWithPositiveGap_acceptsReason() {
        // Arrange
        givenSessionAndLine();
        line.count(new BigDecimal("3"), BigDecimal.ZERO, COUNTED_AT);
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of());

        // Act
        var result = sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.ENTRY_ERROR));

        // Assert
        assertThat(result.gap()).isEqualByComparingTo("3");
        assertThat(result.reason()).isEqualTo(InventoryReason.ENTRY_ERROR);
        assertThat(result.unitCost()).isNull();
    }

    @Test
    void qualifyLine_otherReason_replacesPreviousReason() {
        // Arrange
        givenSessionAndLine();
        line.count(new BigDecimal("10"), new BigDecimal("12"), COUNTED_AT);
        line.qualify(InventoryReason.BREAKAGE);
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of());

        // Act
        var result = sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.LOSS));

        // Assert
        assertThat(result.reason()).isEqualTo(InventoryReason.LOSS);
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "INVENTORY_SESSION",
                        SESSION_ID,
                        "Motif d'écart de ART-0031 dans INV-0012 : Perte ou vol");
    }

    @Test
    void qualifyLine_sameReasonReplayed_changesNothingAndDoesNotJournal() {
        // Arrange
        givenSessionAndLine();
        line.count(new BigDecimal("10"), new BigDecimal("12"), COUNTED_AT);
        line.qualify(InventoryReason.BREAKAGE);
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of());

        // Act
        var result = sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.BREAKAGE));

        // Assert
        assertThat(result.reason()).isEqualTo(InventoryReason.BREAKAGE);
        verify(lineRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void qualifyLine_lineNotCounted_throwsValidationFailedAndChangesNothing() {
        // Arrange
        givenSessionAndLine();

        // Act + Assert
        assertThatThrownBy(() -> sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.LOSS)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ART-0031");
        assertThat(line.getReason()).isNull();
        verify(lineRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void qualifyLine_lineWithoutGap_throwsValidationFailed() {
        // Arrange
        givenSessionAndLine();
        line.count(new BigDecimal("12.000"), new BigDecimal("12"), COUNTED_AT);

        // Act + Assert
        assertThatThrownBy(() -> sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.OTHER)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'est pas en écart");
        verifyNoInteractions(journalService);
    }

    @Test
    void qualifyLine_closedSession_throwsConflictAndChangesNothing() {
        // Arrange
        ReflectionTestUtils.setField(session, "status", InventoryStatus.CLOSED);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        // Act + Assert
        assertThatThrownBy(() -> sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.LOSS)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INV-0012");
        verifyNoInteractions(lineRepository, journalService);
    }

    @Test
    void qualifyLine_unknownSession_throwsNotFound() {
        // Arrange
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.LOSS)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void qualifyLine_lineOfAnotherSession_throwsNotFound() {
        // Arrange
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(lineRepository.findByIdAndSessionId(LINE_ID, SESSION_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.qualifyLine(SESSION_ID, LINE_ID, reason(InventoryReason.LOSS)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(journalService);
    }

    private void givenSessionAndLine() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(lineRepository.findByIdAndSessionId(LINE_ID, SESSION_ID)).thenReturn(Optional.of(line));
    }

    private static InventoryReasonWDto reason(InventoryReason reason) {
        return new InventoryReasonWDto(reason);
    }
}
