package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
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

    @Test
    void recordAll_crossedMovements_locksEveryArticleAndLocationInSortedOrderBeforeRecording() {
        // Arrange
        var otherLocation = UUID.randomUUID();
        var out = transferLeg(otherLocation, "-2");
        var in = transferLeg(LOCATION_ID, "2");
        when(policy.check(any())).thenReturn(StockTestData.article(ARTICLE_ID, "ART-0031"));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(call -> call.getArgument(0));
        var keys = new TreeSet<>(List.of(ARTICLE_ID + ":" + otherLocation, ARTICLE_ID + ":" + LOCATION_ID));

        // Act
        sut.recordAll(List.of(out, in));

        // Assert
        var order = inOrder(stockMovementRepository, policy);
        order.verify(stockMovementRepository).lock(keys.first());
        order.verify(stockMovementRepository).lock(keys.last());
        order.verify(policy).check(out);
        order.verify(policy).check(in);
    }

    private static StockMovementCommand transferLeg(UUID locationId, String quantity) {
        return new StockMovementCommand(
                ARTICLE_ID, locationId, StockMovementType.TRANSFER, new BigDecimal(quantity), null);
    }
}
