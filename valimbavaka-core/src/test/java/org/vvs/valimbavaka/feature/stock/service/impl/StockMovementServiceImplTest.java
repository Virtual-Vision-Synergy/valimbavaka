package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovement;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementRDto;
import org.vvs.valimbavaka.feature.stock.mapper.StockMovementMapper;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceImplTest {

    private static final UUID ARTICLE_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private StockMovementMapper stockMovementMapper;

    @Mock
    private JournalService journalService;

    @Mock
    private StockMovementPolicy policy;

    private StockMovementServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new StockMovementServiceImpl(stockMovementRepository, stockMovementMapper, journalService, policy);
    }

    @Test
    void record_validMovement_savesItAndTracesItInTheJournal() {
        // Arrange
        var command = new StockMovementCommand(
                ARTICLE_ID, LOCATION_ID, StockMovementType.RECEIPT, new BigDecimal("12"), null);
        var saved = StockMovement.of(command);
        var expected = new StockMovementRDto(
                UUID.randomUUID(),
                ARTICLE_ID,
                LOCATION_ID,
                StockMovementType.RECEIPT,
                new BigDecimal("12"),
                null,
                Instant.parse("2026-10-29T08:00:00Z"));
        when(policy.check(command)).thenReturn(StockTestData.article(ARTICLE_ID, "ART-0031"));
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(saved);
        when(stockMovementMapper.toRDto(saved)).thenReturn(expected);

        // Act
        var result = sut.record(command);

        // Assert
        assertThat(result).isSameAs(expected);
        var description = ArgumentCaptor.forClass(String.class);
        verify(journalService).record(eq(JournalAction.CREATE), eq("STOCK_MOVEMENT"), any(), description.capture());
        assertThat(description.getValue()).isEqualTo("Mouvement RECEIPT de l'article ART-0031 · Savon noir 100 g : 12");
    }

    @Test
    void record_rejectedByThePolicy_savesNothingAndTracesNothing() {
        // Arrange
        var command =
                new StockMovementCommand(ARTICLE_ID, LOCATION_ID, StockMovementType.SALE, new BigDecimal("-5"), null);
        when(policy.check(command)).thenThrow(new ConflictException("Stock insuffisant"));

        // Act + Assert
        assertThatThrownBy(() -> sut.record(command)).isInstanceOf(ConflictException.class);
        verify(stockMovementRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }
}
