package org.vvs.valimbavaka.feature.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryArticle;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryReason;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryScope;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.inventory.service.InventoryService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

@ExtendWith(MockitoExtension.class)
class InventoryClosingServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-08T14:30:00Z");
    private static final Instant COUNTED_AT = Instant.parse("2026-11-08T07:15:00Z");
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = InventoryTestData.LOCATION_ID;

    @Mock
    private InventorySessionRepository sessionRepository;

    @Mock
    private InventoryLineRepository lineRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private StockService stockService;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private JournalService journalService;

    private InventorySession session;
    private InventoryClosingServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryClosingServiceImpl(
                sessionRepository,
                lineRepository,
                inventoryService,
                stockService,
                stockMovementService,
                journalService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        session = InventorySession.open("INV-0012", InventoryTestData.opening(null));
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
    }

    @Test
    void closeSession_countedLinesWithGaps_recordsSignedInventoryMovementsAndCloses() {
        // Arrange
        var missing = line("ART-0031", "10", "12", InventoryReason.BREAKAGE);
        var exact = line("ART-0032", "5", "5", null);
        var surplus = line("ART-0033", "3", "0", InventoryReason.ENTRY_ERROR);
        givenLines(missing, exact, surplus);
        givenStock(Map.of(missing.getArticleId(), new BigDecimal("12"), exact.getArticleId(), new BigDecimal("5")));
        givenClosedView(3);

        // Act
        var result = sut.closeSession(SESSION_ID);

        // Assert
        var commands = recordedCommands(2);
        assertThat(commands)
                .extracting(StockMovementCommand::articleId)
                .containsExactly(missing.getArticleId(), surplus.getArticleId());
        assertThat(commands.get(0).quantity()).isEqualByComparingTo("-2");
        assertThat(commands.get(1).quantity()).isEqualByComparingTo("3");
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.type()).isEqualTo(StockMovementType.INVENTORY);
            assertThat(command.locationId()).isEqualTo(LOCATION_ID);
            assertThat(command.referenceId()).isEqualTo(SESSION_ID);
        });
        assertThat(session.getStatus()).isEqualTo(InventoryStatus.CLOSED);
        assertThat(session.getClosedAt()).isEqualTo(NOW);
        assertThat(result.status()).isEqualTo(InventoryStatus.CLOSED);
        verify(sessionRepository).saveAndFlush(session);
        verify(journalService).record(JournalAction.UPDATE, "INVENTORY_SESSION", SESSION_ID, closingLabel(3, 2));
    }

    @Test
    void closeSession_saleAfterCount_adjustsToCurrentStockNotToFrozenGap() {
        // Arrange
        var line = line("ART-0031", "10", "12", InventoryReason.LOSS);
        givenLines(line);
        givenStock(Map.of(line.getArticleId(), new BigDecimal("11")));
        givenClosedView(1);

        // Act
        sut.closeSession(SESSION_ID);

        // Assert
        assertThat(recordedCommands(1).get(0).quantity()).isEqualByComparingTo("-1");
    }

    @Test
    void closeSession_decimalCountOfRawMaterial_recordsDecimalAdjustment() {
        // Arrange
        var line = line("ART-0040", "2.5", "3", InventoryReason.OTHER);
        givenLines(line);
        givenStock(Map.of(line.getArticleId(), new BigDecimal("3.000")));
        givenClosedView(1);

        // Act
        sut.closeSession(SESSION_ID);

        // Assert
        assertThat(recordedCommands(1).get(0).quantity()).isEqualByComparingTo("-0.5");
    }

    @Test
    void closeSession_noGap_closesWithoutAnyMovement() {
        // Arrange
        var line = line("ART-0032", "5", "5", null);
        givenLines(line);
        givenStock(Map.of(line.getArticleId(), new BigDecimal("5")));
        givenClosedView(1);

        // Act
        sut.closeSession(SESSION_ID);

        // Assert
        verifyNoInteractions(stockMovementService);
        assertThat(session.getStatus()).isEqualTo(InventoryStatus.CLOSED);
        verify(journalService).record(JournalAction.UPDATE, "INVENTORY_SESSION", SESSION_ID, closingLabel(1, 0));
    }

    @Test
    void closeSession_articleNeverMovedInLocation_adjustsFromZero() {
        // Arrange
        var line = line("ART-0050", "4", "0", InventoryReason.UNDECLARED_SHOOTING);
        givenLines(line);
        givenStock(Map.of());
        givenClosedView(1);

        // Act
        sut.closeSession(SESSION_ID);

        // Assert
        assertThat(recordedCommands(1).get(0).quantity()).isEqualByComparingTo("4");
    }

    @Test
    void closeSession_lineNotCounted_throwsValidationFailedAndChangesNothing() {
        // Arrange
        var counted = line("ART-0031", "10", "10", null);
        var notCounted = InventoryLine.toCount(SESSION_ID, article("ART-0032"));
        givenLines(counted, notCounted);

        // Act + Assert
        assertThatThrownBy(() -> sut.closeSession(SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("non compté(s) ART-0032 (1)");
        assertThat(session.getStatus()).isEqualTo(InventoryStatus.IN_PROGRESS);
        verify(sessionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(stockService, stockMovementService, journalService);
    }

    @Test
    void closeSession_moreThanFiveLinesNotCounted_listsFiveCodesAndTheTotal() {
        // Arrange
        var lines = new ArrayList<InventoryLine>();
        for (var index = 1; index <= 6; index++) {
            lines.add(InventoryLine.toCount(SESSION_ID, article("ART-000" + index)));
        }
        givenLines(lines.toArray(InventoryLine[]::new));

        // Act + Assert
        assertThatThrownBy(() -> sut.closeSession(SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ART-0001, ART-0002, ART-0003, ART-0004, ART-0005… (6)")
                .hasMessageNotContaining("ART-0006");
    }

    @Test
    void closeSession_gapWithoutReason_throwsValidationFailedAndChangesNothing() {
        // Arrange
        givenLines(line("ART-0031", "10", "12", null));

        // Act + Assert
        assertThatThrownBy(() -> sut.closeSession(SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sans motif ART-0031 (1)");
        assertThat(session.getStatus()).isEqualTo(InventoryStatus.IN_PROGRESS);
        verifyNoInteractions(stockService, stockMovementService, journalService);
    }

    @Test
    void closeSession_alreadyClosed_throwsConflictAndChangesNothing() {
        // Arrange
        session.close(COUNTED_AT);
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        // Act + Assert
        assertThatThrownBy(() -> sut.closeSession(SESSION_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INV-0012");
        assertThat(session.getClosedAt()).isEqualTo(COUNTED_AT);
        verifyNoInteractions(lineRepository, stockMovementService, journalService);
    }

    @Test
    void closeSession_cancelledSession_throwsConflict() {
        // Arrange
        ReflectionTestUtils.setField(session, "status", InventoryStatus.CANCELLED);
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        // Act + Assert
        assertThatThrownBy(() -> sut.closeSession(SESSION_ID)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(stockMovementService);
    }

    @Test
    void closeSession_unknownSession_throwsNotFound() {
        // Arrange
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.closeSession(SESSION_ID)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(lineRepository, stockMovementService, journalService);
    }

    private void givenLines(InventoryLine... lines) {
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(lineRepository.findBySessionIdOrderByArticleCode(SESSION_ID)).thenReturn(List.of(lines));
    }

    private void givenStock(Map<UUID, BigDecimal> quantities) {
        when(stockService.getQuantities(eq(LOCATION_ID), anyCollection())).thenReturn(quantities);
    }

    private void givenClosedView(long lineCount) {
        when(inventoryService.getSession(SESSION_ID)).thenReturn(closedView(lineCount));
    }

    private List<StockMovementCommand> recordedCommands(int expected) {
        var captor = ArgumentCaptor.forClass(StockMovementCommand.class);
        verify(stockMovementService, times(expected)).record(captor.capture());
        return captor.getAllValues();
    }

    private static String closingLabel(int articles, int adjustments) {
        return "Clôture de la session d'inventaire INV-0012 · Boutique : %d article(s), %d ajustement(s) de stock"
                .formatted(articles, adjustments);
    }

    private static InventoryLine line(String code, String counted, String expected, @Nullable InventoryReason reason) {
        var line = InventoryLine.toCount(SESSION_ID, article(code));
        line.count(new BigDecimal(counted), new BigDecimal(expected), COUNTED_AT);
        if (reason != null) {
            line.qualify(reason);
        }
        return line;
    }

    private static InventoryArticle article(String code) {
        return new InventoryArticle(UUID.randomUUID(), code, "Savon " + code, null);
    }

    private static InventorySessionRDto closedView(long lineCount) {
        return new InventorySessionRDto(
                SESSION_ID,
                "INV-0012",
                LOCATION_ID,
                "STORE",
                "Boutique",
                InventoryScope.ALL,
                null,
                UUID.randomUUID(),
                "Hanta Rakoto",
                LocalDate.of(2026, 11, 8),
                InventoryStatus.CLOSED,
                COUNTED_AT,
                null,
                NOW,
                lineCount,
                1L);
    }
}
