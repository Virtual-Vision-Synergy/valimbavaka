package org.vvs.valimbavaka.feature.sale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.PricedSaleLine;
import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleHeader;
import org.vvs.valimbavaka.feature.sale.domain.SaleRecordedEvent;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;

@ExtendWith(MockitoExtension.class)
class SaleLedgerTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private JournalService journalService;

    @Mock
    private ApplicationEventPublisher events;

    @Captor
    private ArgumentCaptor<List<StockMovementCommand>> movements;

    private SaleLedger sut;

    @BeforeEach
    void setUp() {
        sut = new SaleLedger(stockMovementService, journalService, events);
    }

    @Test
    void post_twoLines_takesTheStockOutOfTheChannelLocationTracesAndPublishes() {
        // Arrange
        var header = new SaleHeader(
                UUID.randomUUID(),
                LocalDate.of(2026, 11, 8),
                UUID.randomUUID(),
                STORE,
                UUID.randomUUID(),
                PaymentMethod.MVOLA);
        var lines = List.of(
                new PricedSaleLine(SOAP, new BigDecimal("3"), new BigDecimal("8500"), null),
                new PricedSaleLine(OIL, new BigDecimal("1"), new BigDecimal("12000"), null));
        var sale = Sale.record("VTE-0042", header, lines);

        // Act
        sut.post(sale, "Boutique");

        // Assert
        verify(stockMovementService).recordAll(movements.capture());
        assertThat(movements.getValue())
                .extracting(StockMovementCommand::articleId, StockMovementCommand::type)
                .containsExactly(tuple(SOAP, StockMovementType.SALE), tuple(OIL, StockMovementType.SALE));
        assertThat(movements.getValue())
                .extracting(StockMovementCommand::locationId)
                .containsOnly(STORE);
        assertThat(movements.getValue().getFirst().quantity()).isEqualByComparingTo("-3");
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("SALE"),
                        eq(sale.getId()),
                        eq("Vente VTE-0042 · Boutique · 2 article(s) · 37500.00 Ar · MVola"));
        verify(events).publishEvent(new SaleRecordedEvent(sale.getId(), "VTE-0042"));
    }
}
