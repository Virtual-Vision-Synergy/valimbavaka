package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.repository.StockBalance;
import org.vvs.valimbavaka.feature.stock.repository.StockMovementRepository;
import org.vvs.valimbavaka.feature.stock.repository.StockTypeTotal;

@ExtendWith(MockitoExtension.class)
class StockDayLedgerServiceImplTest {

    private static final Instant FROM = Instant.parse("2026-11-10T21:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-11T21:00:00Z");

    @Mock
    private StockMovementRepository stockMovementRepository;

    private StockDayLedgerServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new StockDayLedgerServiceImpl(stockMovementRepository);
    }

    @Test
    void getMovementTotals_locationsGiven_mapsTheRepositoryRows() {
        // Arrange
        var article = UUID.randomUUID();
        var store = UUID.randomUUID();
        var total = new StockTypeTotal(article, store, StockMovementType.SALE, new BigDecimal("-3"));
        when(stockMovementRepository.totalsByType(FROM, TO, List.of(store))).thenReturn(List.of(total));

        // Act
        var totals = sut.getMovementTotals(FROM, TO, List.of(store));

        // Assert
        assertThat(totals).singleElement().satisfies(mapped -> {
            assertThat(mapped.articleId()).isEqualTo(article);
            assertThat(mapped.locationId()).isEqualTo(store);
            assertThat(mapped.type()).isEqualTo(StockMovementType.SALE);
            assertThat(mapped.quantity()).isEqualByComparingTo("-3");
        });
    }

    @Test
    void getMovementTotals_noLocation_returnsEmptyWithoutReading() {
        // Act
        var totals = sut.getMovementTotals(FROM, TO, List.of());

        // Assert
        assertThat(totals).isEmpty();
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void getBalancesBefore_locationsGiven_mapsTheRepositoryRows() {
        // Arrange
        var article = UUID.randomUUID();
        var store = UUID.randomUUID();
        var balance = new StockBalance(article, store, new BigDecimal("12"));
        when(stockMovementRepository.balancesBefore(FROM, List.of(store))).thenReturn(List.of(balance));

        // Act
        var balances = sut.getBalancesBefore(FROM, List.of(store));

        // Assert
        assertThat(balances).singleElement().satisfies(mapped -> {
            assertThat(mapped.articleId()).isEqualTo(article);
            assertThat(mapped.locationId()).isEqualTo(store);
            assertThat(mapped.quantity()).isEqualByComparingTo("12");
        });
    }

    @Test
    void getBalancesBefore_noLocation_returnsEmptyWithoutReading() {
        // Act
        var balances = sut.getBalancesBefore(FROM, List.of());

        // Assert
        assertThat(balances).isEmpty();
        verifyNoInteractions(stockMovementRepository);
    }
}
