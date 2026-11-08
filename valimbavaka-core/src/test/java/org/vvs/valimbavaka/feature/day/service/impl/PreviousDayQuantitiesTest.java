package org.vvs.valimbavaka.feature.day.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.day.domain.BusinessDay;
import org.vvs.valimbavaka.feature.day.dto.DayQuantityRDto;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;
import org.vvs.valimbavaka.feature.transfer.dto.TransferLineRDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;

@ExtendWith(MockitoExtension.class)
class PreviousDayQuantitiesTest {

    private static final LocalDate YESTERDAY = LocalDate.of(2026, 11, 7);
    private static final UUID WAREHOUSE = UUID.randomUUID();
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID DELIVERY = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Mock
    private TransferService transferService;

    private PreviousDayQuantities sut;

    @BeforeEach
    void setUp() {
        sut = new PreviousDayQuantities(transferService);
    }

    @Test
    void of_noPreviousDay_returnsEmptyWithoutReadingTransfers() {
        // Act
        var result = sut.of(null);

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(transferService);
    }

    @Test
    void of_previousDayWithoutMorningTransfer_returnsEmptyWithoutReadingTransfers() {
        // Act
        var result = sut.of(BusinessDay.open(YESTERDAY));

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(transferService);
    }

    @Test
    void of_twoMorningTransfers_groupsTheQuantitiesByArticleAndDestination() {
        // Arrange
        var storeTransfer = transfer(STORE, line(SOAP, "12"), line(OIL, "4"));
        var deliveryTransfer = transfer(DELIVERY, line(SOAP, "5"));
        var day = BusinessDay.open(YESTERDAY);
        day.addTransfer(storeTransfer.id(), "TRF-0093", STORE, List.of(new BigDecimal("12"), new BigDecimal("4")));
        day.addTransfer(deliveryTransfer.id(), "TRF-0094", DELIVERY, List.of(new BigDecimal("5")));
        var ids = List.of(storeTransfer.id(), deliveryTransfer.id());
        when(transferService.getTransfersByIds(ids)).thenReturn(List.of(deliveryTransfer, storeTransfer));

        // Act
        var result = sut.of(day);

        // Assert
        var soapToStore = new DayQuantityRDto(STORE, new BigDecimal("12"));
        var soapToDelivery = new DayQuantityRDto(DELIVERY, new BigDecimal("5"));
        assertThat(result).containsOnlyKeys(SOAP, OIL);
        assertThat(result.get(SOAP)).containsExactlyInAnyOrder(soapToStore, soapToDelivery);
        assertThat(result.get(OIL)).containsExactly(new DayQuantityRDto(STORE, new BigDecimal("4")));
    }

    private static TransferLineRDto line(UUID articleId, String quantity) {
        return new TransferLineRDto(articleId, new BigDecimal(quantity));
    }

    private static TransferRDto transfer(UUID destinationId, TransferLineRDto... lines) {
        return new TransferRDto(
                UUID.randomUUID(),
                "TRF-0093",
                TransferOrigin.MORNING,
                TransferStatus.VALIDATED,
                WAREHOUSE,
                destinationId,
                null,
                List.of(lines),
                Instant.parse("2026-11-07T05:00:00Z"),
                null);
    }
}
