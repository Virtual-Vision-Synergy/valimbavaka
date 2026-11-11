package org.vvs.valimbavaka.feature.commission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.commission.domain.CommissionSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.SaleCancelledEvent;
import org.vvs.valimbavaka.feature.sale.domain.SaleRecordedEvent;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class SaleCommissionListenerTest {

    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 8);

    @Mock
    private SaleService saleService;

    @Mock
    private CommissionService commissionService;

    private SaleCommissionListener sut;

    @BeforeEach
    void setUp() {
        sut = new SaleCommissionListener(saleService, commissionService);
    }

    @Test
    void onSaleRecorded_saleWithSeller_calculatesOnTheTotalWithThePurchaseCost() {
        // Arrange
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(SELLER_ID));
        when(saleService.getPurchaseCost(SALE_ID)).thenReturn(new BigDecimal("9361.50"));

        // Act
        sut.onSaleRecorded(new SaleRecordedEvent(SALE_ID, "VTE-0042"));

        // Assert
        var source = ArgumentCaptor.forClass(CommissionSource.class);
        verify(commissionService).calculate(source.capture());
        assertThat(source.getValue())
                .isEqualTo(new CommissionSource(
                        CommissionSourceType.SALE,
                        SALE_ID,
                        "VTE-0042",
                        SELLER_ID,
                        CHANNEL_ID,
                        null,
                        DAY,
                        new BigDecimal("25500.00"),
                        new BigDecimal("9361.50")));
    }

    @Test
    void onSaleCancelled_anySale_cancelsTheCommissionOfTheSale() {
        // Act
        sut.onSaleCancelled(new SaleCancelledEvent(SALE_ID, "VTE-0042"));

        // Assert
        verify(commissionService).cancelBySource(CommissionSourceType.SALE, SALE_ID);
    }

    @Test
    void onSaleRecorded_saleWithoutSeller_calculatesNothing() {
        // Arrange : commande saisie par l'administrateur (LIV-03), décision PO du 07/11/2026
        when(saleService.getSaleById(SALE_ID)).thenReturn(sale(null));

        // Act
        sut.onSaleRecorded(new SaleRecordedEvent(SALE_ID, "VTE-0042"));

        // Assert
        verifyNoInteractions(commissionService);
        verify(saleService, never()).getPurchaseCost(SALE_ID);
    }

    private static SaleRDto sale(@Nullable UUID sellerId) {
        var total = new BigDecimal("25500.00");
        return new SaleRDto(
                SALE_ID,
                "VTE-0042",
                UUID.randomUUID(),
                DAY,
                CHANNEL_ID,
                UUID.randomUUID(),
                sellerId,
                PaymentMethod.CASH,
                OperationStatus.VALIDATED,
                new BigDecimal("28000.00"),
                new BigDecimal("2500.00"),
                UUID.randomUUID(),
                total,
                List.of(),
                Instant.now(),
                null);
    }
}
