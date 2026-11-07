package org.vvs.valimbavaka.feature.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryArticle;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryCountWDto;
import org.vvs.valimbavaka.feature.inventory.mapper.InventoryMapper;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

@ExtendWith(MockitoExtension.class)
class InventoryCountServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-08T07:15:00Z");
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID LINE_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();

    @Mock
    private InventorySessionRepository sessionRepository;

    @Mock
    private InventoryLineRepository lineRepository;

    @Mock
    private StockService stockService;

    @Mock
    private ArticleService articleService;

    @Mock
    private JournalService journalService;

    private InventorySession session;
    private InventoryLine line;
    private InventoryCountServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryCountServiceImpl(
                sessionRepository,
                lineRepository,
                new InventoryMapper(),
                stockService,
                articleService,
                journalService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        session = InventorySession.open("INV-0012", InventoryTestData.opening(null));
        line = InventoryLine.toCount(
                SESSION_ID, new InventoryArticle(ARTICLE_ID, "ART-0031", "Savon noir 100 g", "3210000000031"));
    }

    @Test
    void countLine_quantityBelowStock_freezesTheoreticalReturnsNegativeGapAndJournals() {
        // Arrange
        givenSessionAndLine();
        givenStock("12");
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of(ARTICLE_ID, new BigDecimal("3120.50")));

        // Act
        var result = sut.countLine(SESSION_ID, LINE_ID, count("10"));

        // Assert
        assertThat(result.theoreticalQuantity()).isEqualByComparingTo("12");
        assertThat(result.countedQuantity()).isEqualByComparingTo("10");
        assertThat(result.gap()).isEqualByComparingTo("-2");
        assertThat(result.unitCost()).isEqualByComparingTo("3120.50");
        assertThat(line.getExpectedQuantity()).isEqualByComparingTo("12");
        assertThat(line.getCountedAt()).isEqualTo(NOW);
        verify(lineRepository).save(line);
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "INVENTORY_SESSION",
                        SESSION_ID,
                        "Comptage de ART-0031 dans INV-0012 : 10 compté(s), 12 attendu(s), écart -2");
    }

    @Test
    void countLine_articleAbsentFromLocation_usesZeroAsTheoreticalAndReturnsPositiveGap() {
        // Arrange
        givenSessionAndLine();
        when(stockService.getQuantities(any(), anyCollection())).thenReturn(Map.of());
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of());

        // Act
        var result = sut.countLine(SESSION_ID, LINE_ID, count("3"));

        // Assert
        assertThat(result.theoreticalQuantity()).isEqualByComparingTo("0");
        assertThat(result.gap()).isEqualByComparingTo("3");
        assertThat(result.unitCost()).isNull();
    }

    @Test
    void countLine_zeroCountedEqualToStock_returnsZeroGap() {
        // Arrange
        givenSessionAndLine();
        givenStock("0");
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of());

        // Act
        var result = sut.countLine(SESSION_ID, LINE_ID, count("0"));

        // Assert
        assertThat(result.gap()).isEqualByComparingTo("0");
    }

    @Test
    void countLine_recount_replacesCountedAndRefreezesTheoretical() {
        // Arrange
        givenSessionAndLine();
        line.count(new BigDecimal("10"), new BigDecimal("12"), NOW.minusSeconds(600));
        givenStock("11");
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of());

        // Act
        var result = sut.countLine(SESSION_ID, LINE_ID, count("11"));

        // Assert
        assertThat(result.theoreticalQuantity()).isEqualByComparingTo("11");
        assertThat(result.gap()).isEqualByComparingTo("0");
        assertThat(line.getCountedAt()).isEqualTo(NOW);
    }

    @Test
    void countLine_sameCountReplayed_changesNothingAndDoesNotJournal() {
        // Arrange
        givenSessionAndLine();
        line.count(new BigDecimal("10.000"), new BigDecimal("12"), NOW.minusSeconds(600));
        givenStock("12");
        when(articleService.getCosts(anyCollection())).thenReturn(Map.of());

        // Act
        var result = sut.countLine(SESSION_ID, LINE_ID, count("10"));

        // Assert
        assertThat(result.gap()).isEqualByComparingTo("-2");
        assertThat(line.getCountedAt()).isEqualTo(NOW.minusSeconds(600));
        verify(lineRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void countLine_closedSession_throwsConflictAndChangesNothing() {
        // Arrange
        ReflectionTestUtils.setField(session, "status", InventoryStatus.CLOSED);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        // Act + Assert
        assertThatThrownBy(() -> sut.countLine(SESSION_ID, LINE_ID, count("10")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INV-0012");
        verifyNoInteractions(lineRepository, stockService, journalService);
    }

    @Test
    void countLine_unknownSession_throwsNotFound() {
        // Arrange
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.countLine(SESSION_ID, LINE_ID, count("10")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void countLine_lineOfAnotherSession_throwsNotFound() {
        // Arrange
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(lineRepository.findByIdAndSessionId(LINE_ID, SESSION_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.countLine(SESSION_ID, LINE_ID, count("10")))
                .isInstanceOf(NotFoundException.class);
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    private void givenSessionAndLine() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(lineRepository.findByIdAndSessionId(LINE_ID, SESSION_ID)).thenReturn(Optional.of(line));
    }

    private void givenStock(String quantity) {
        when(stockService.getQuantities(eq(InventoryTestData.LOCATION_ID), anyCollection()))
                .thenReturn(Map.of(ARTICLE_ID, new BigDecimal(quantity)));
    }

    private static InventoryCountWDto count(String quantity) {
        return new InventoryCountWDto(new BigDecimal(quantity));
    }
}
