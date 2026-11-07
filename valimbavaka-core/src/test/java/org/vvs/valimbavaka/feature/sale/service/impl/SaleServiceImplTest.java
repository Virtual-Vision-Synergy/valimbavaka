package org.vvs.valimbavaka.feature.sale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.PricedSaleLine;
import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.dto.SaleChannelOptionRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleLineWDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleWDto;
import org.vvs.valimbavaka.feature.sale.mapper.SaleMapper;
import org.vvs.valimbavaka.feature.sale.repository.SaleRepository;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final UUID DAY = UUID.randomUUID();
    private static final UUID CHANNEL = UUID.randomUUID();
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleMapper saleMapper;

    @Mock
    private BusinessDayService businessDayService;

    @Mock
    private SalePolicy policy;

    @Mock
    private SaleLedger ledger;

    @Mock
    private SaleFormAssembler formAssembler;

    private SaleServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SaleServiceImpl(saleRepository, saleMapper, businessDayService, policy, ledger, formAssembler);
    }

    @Test
    void createSale_openDay_recordsTheSaleInTheDayAndTakesTheStockOfTheChannelLocation() {
        // Arrange
        when(businessDayService.requireOpenDay()).thenReturn(openDay());
        var line = new PricedSaleLine(SOAP, new BigDecimal("2"), new BigDecimal("8500"), null);
        var channel = new SaleChannelOptionRDto(CHANNEL, "FACEBOOK", "Facebook", STORE);
        when(policy.check(any())).thenReturn(new CheckedSale(channel, List.of(line)));
        when(saleRepository.nextNumber()).thenReturn(42L);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new SaleWDto(
                CHANNEL, SELLER, PaymentMethod.CARD, List.of(new SaleLineWDto(SOAP, new BigDecimal("2"))));

        // Act
        sut.createSale(request);

        // Assert
        var commands = ArgumentCaptor.forClass(SaleCommand.class);
        verify(policy).check(commands.capture());
        assertThat(commands.getValue().lines().getFirst().unitPrice()).isNull();
        var sales = ArgumentCaptor.forClass(Sale.class);
        verify(ledger).post(sales.capture(), eq("Facebook"));
        var sale = sales.getValue();
        assertThat(sale.getNumber()).isEqualTo("VTE-0042");
        assertThat(sale.getBusinessDayId()).isEqualTo(DAY);
        assertThat(sale.getBusinessDate()).isEqualTo(TODAY);
        assertThat(sale.getStockLocationId()).isEqualTo(STORE);
        assertThat(sale.getSellerId()).isEqualTo(SELLER);
        assertThat(sale.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(sale.getTotal()).isEqualByComparingTo("17000");
    }

    @Test
    void record_dayNotOpened_throwsConflictAndRecordsNothing() {
        // Arrange
        when(businessDayService.requireOpenDay())
                .thenThrow(new ConflictException("La journée du 08/11/2026 n'est pas ouverte."));
        var command = new SaleCommand(CHANNEL, SELLER, PaymentMethod.CASH, List.of());

        // Act + Assert
        assertThatThrownBy(() -> sut.record(command)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(policy, saleRepository, ledger);
    }

    private static BusinessDayRDto openDay() {
        return new BusinessDayRDto(DAY, TODAY, DayStatus.OPEN, List.of(), Instant.EPOCH, null);
    }
}
