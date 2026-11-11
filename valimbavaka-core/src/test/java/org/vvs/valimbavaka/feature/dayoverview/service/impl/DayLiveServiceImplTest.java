package org.vvs.valimbavaka.feature.dayoverview.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayoverview.domain.DayFeedType;
import org.vvs.valimbavaka.feature.dayoverview.dto.DayFeedItemRDto;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryDayService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.SaleFilter;
import org.vvs.valimbavaka.feature.sale.dto.ChannelDashboardRDto;
import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryRDto;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;
import org.vvs.valimbavaka.feature.sale.service.SaleQueryService;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class DayLiveServiceImplTest {

    /** 13:00 in Antananarivo (UTC+3), 2026-11-11. */
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static final Instant START_OF_TODAY = Instant.parse("2026-11-10T21:00:00Z");
    private static final Instant START_OF_TOMORROW = Instant.parse("2026-11-11T21:00:00Z");
    private static final UUID STORE = UUID.randomUUID();

    @Mock
    private BusinessDayService businessDayService;

    @Mock
    private ChannelDashboardService channelDashboardService;

    @Mock
    private SaleQueryService saleQueryService;

    @Mock
    private DeliveryDayService deliveryDayService;

    @Mock
    private ServiceRecordService serviceRecordService;

    private DayLiveServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = serviceAt(NOW);
    }

    @Test
    void getLive_activeDay_returnsIndicatorsOfTheThreeActivities() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubSales(List.of(sale("VTE-0002", "6000", OperationStatus.VALIDATED, "2026-11-11T09:30:00Z")));
        stubDeliveries(Map.of(DeliveryStatus.ON_ROUND, 2L, DeliveryStatus.DELIVERED, 3L), List.of());
        stubServices(List.of(
                service("PRE-0001", "35000", "2026-11-11T08:00:00Z"),
                service("PRE-0002", "20000", "2026-11-11T08:30:00Z")));

        // Act
        var live = sut.getLive();

        // Assert
        assertThat(live.businessDate()).isEqualTo(TODAY);
        assertThat(live.status()).isEqualTo(DayStatus.OPEN);
        assertThat(live.generatedAt()).isEqualTo(NOW);
        assertThat(live.sales().revenue()).isEqualByComparingTo("15500");
        assertThat(live.sales().count()).isEqualTo(3);
        assertThat(live.sales().channels()).hasSize(1);
        assertThat(live.deliveries().total()).isEqualTo(5);
        assertThat(live.deliveries().onRound()).isEqualTo(2);
        assertThat(live.deliveries().delivered()).isEqualTo(3);
        assertThat(live.deliveries().toPrepare()).isZero();
        assertThat(live.services().count()).isEqualTo(2);
        assertThat(live.services().revenue()).isEqualByComparingTo("55000");
    }

    @Test
    void getLive_dayNotOpenedYet_hasNoStatusAndEmptyIndicators() {
        // Arrange
        when(businessDayService.findToday()).thenReturn(null);
        stubSales(List.of());
        stubDeliveries(Map.of(), List.of());
        stubServices(List.of());

        // Act
        var live = sut.getLive();

        // Assert
        assertThat(live.status()).isNull();
        assertThat(live.sales().count()).isZero();
        assertThat(live.deliveries().total()).isZero();
        assertThat(live.services().revenue()).isEqualByComparingTo("0");
        assertThat(live.feed()).isEmpty();
    }

    @Test
    void getLive_operationsOfTheThreeActivities_areMergedMostRecentFirst() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubSales(List.of(
                sale("VTE-0002", "6000", OperationStatus.VALIDATED, "2026-11-11T09:00:00Z"),
                sale("VTE-0001", "2500", OperationStatus.CANCELLED, "2026-11-11T07:00:00Z")));
        stubDeliveries(
                Map.of(DeliveryStatus.DELIVERED, 1L),
                List.of(delivery("LIV-0001", DeliveryStatus.DELIVERED, "2026-11-11T08:00:00Z")));
        stubServices(List.of(service("PRE-0001", "35000", "2026-11-11T09:30:00Z")));

        // Act
        var feed = sut.getLive().feed();

        // Assert
        assertThat(feed)
                .extracting(DayFeedItemRDto::reference)
                .containsExactly("PRE-0001", "VTE-0002", "LIV-0001", "VTE-0001");
        assertThat(feed.get(0).type()).isEqualTo(DayFeedType.SERVICE);
        assertThat(feed.get(0).status()).isNull();
        assertThat(feed.get(2).status()).isEqualTo("DELIVERED");
        assertThat(feed.get(3).status()).isEqualTo("CANCELLED");
        assertThat(feed.get(1).actorName()).isEqualTo("Miora Rakoto");
    }

    @Test
    void getLive_moreOperationsThanTheFeedLimit_keepsTheLatestOnes() {
        // Arrange
        stubDay(DayStatus.OPEN);
        var sales = new ArrayList<SaleSummaryRDto>();
        for (var minute = 0; minute < DayLiveServiceImpl.FEED_LIMIT; minute++) {
            var time = Instant.parse("2026-11-11T06:00:00Z").plusSeconds(60L * minute);
            sales.add(sale("VTE-%04d".formatted(minute), "1000", OperationStatus.VALIDATED, time.toString()));
        }
        stubSales(sales);
        stubDeliveries(Map.of(), List.of());
        stubServices(List.of(service("PRE-0001", "35000", "2026-11-11T05:00:00Z")));

        // Act
        var feed = sut.getLive().feed();

        // Assert
        assertThat(feed).hasSize(DayLiveServiceImpl.FEED_LIMIT);
        assertThat(feed.get(0).reference()).isEqualTo("VTE-0029");
        assertThat(feed).extracting(DayFeedItemRDto::reference).doesNotContain("PRE-0001");
    }

    @Test
    void getLive_lateEveningUtc_isAlreadyTomorrowInAntananarivo() {
        // Arrange
        var lateEvening = Instant.parse("2026-11-11T21:30:00Z");
        sut = serviceAt(lateEvening);
        var tomorrow = TODAY.plusDays(1);
        var dashboard = new ChannelDashboardRDto(tomorrow, tomorrow, BigDecimal.ZERO, 0, List.of());
        when(businessDayService.findToday()).thenReturn(null);
        when(channelDashboardService.getDashboard(tomorrow, tomorrow)).thenReturn(dashboard);
        when(saleQueryService.getSales(any(), eq(new SaleFilter(tomorrow, null, null))))
                .thenReturn(new PageResponse<>(List.of(), 1, 30, 0, 0));
        when(deliveryDayService.getRecentOrders(tomorrow, DayLiveServiceImpl.FEED_LIMIT))
                .thenReturn(List.of());
        when(deliveryDayService.countByStatus(tomorrow)).thenReturn(allStatuses(Map.of()));
        when(serviceRecordService.getServiceRecordsBetween(
                        Instant.parse("2026-11-11T21:00:00Z"), Instant.parse("2026-11-12T21:00:00Z")))
                .thenReturn(List.of());

        // Act
        var live = sut.getLive();

        // Assert
        assertThat(live.businessDate()).isEqualTo(tomorrow);
    }

    private DayLiveServiceImpl serviceAt(Instant instant) {
        return new DayLiveServiceImpl(
                businessDayService,
                channelDashboardService,
                saleQueryService,
                deliveryDayService,
                serviceRecordService,
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void stubDay(DayStatus status) {
        var day = new BusinessDayRDto(UUID.randomUUID(), TODAY, status, List.of(), NOW, null);
        when(businessDayService.findToday()).thenReturn(day);
    }

    private void stubSales(List<SaleSummaryRDto> sales) {
        var channel = new ChannelSalesRDto(STORE, "Boutique", new BigDecimal("15500"), 3, new BigDecimal("100.0"));
        var count = sales.isEmpty() ? 0 : 3;
        var revenue = sales.isEmpty() ? BigDecimal.ZERO : new BigDecimal("15500");
        var channels = sales.isEmpty() ? List.<ChannelSalesRDto>of() : List.of(channel);
        when(channelDashboardService.getDashboard(TODAY, TODAY))
                .thenReturn(new ChannelDashboardRDto(TODAY, TODAY, revenue, count, channels));
        when(saleQueryService.getSales(any(), eq(new SaleFilter(TODAY, null, null))))
                .thenReturn(new PageResponse<>(sales, 1, 30, sales.size(), 1));
    }

    /** Like the real service: every status is present, at zero when no order has it. */
    private static Map<DeliveryStatus, Long> allStatuses(Map<DeliveryStatus, Long> counts) {
        var complete = new EnumMap<DeliveryStatus, Long>(DeliveryStatus.class);
        for (var status : DeliveryStatus.values()) {
            complete.put(status, counts.getOrDefault(status, 0L));
        }
        return complete;
    }

    private void stubDeliveries(Map<DeliveryStatus, Long> counts, List<DeliveryOrderRDto> orders) {
        when(deliveryDayService.countByStatus(TODAY)).thenReturn(allStatuses(counts));
        when(deliveryDayService.getRecentOrders(TODAY, DayLiveServiceImpl.FEED_LIMIT))
                .thenReturn(orders);
    }

    private void stubServices(List<ServiceRecordRDto> records) {
        when(serviceRecordService.getServiceRecordsBetween(START_OF_TODAY, START_OF_TOMORROW))
                .thenReturn(records);
    }

    private static SaleSummaryRDto sale(String number, String total, OperationStatus status, String at) {
        var amount = new BigDecimal(total);
        return new SaleSummaryRDto(
                UUID.randomUUID(),
                number,
                TODAY,
                Instant.parse(at),
                STORE,
                "Boutique",
                UUID.randomUUID(),
                "Miora Rakoto",
                PaymentMethod.CASH,
                status,
                amount,
                BigDecimal.ZERO,
                amount,
                List.of());
    }

    private static ServiceRecordRDto service(String number, String price, String at) {
        return new ServiceRecordRDto(
                UUID.randomUUID(),
                number,
                UUID.randomUUID(),
                "Soin du visage",
                45,
                new BigDecimal(price),
                "Hanta",
                UUID.randomUUID(),
                "Miora Rakoto",
                STORE,
                UUID.randomUUID(),
                List.of(),
                null,
                Instant.parse(at));
    }

    private static DeliveryOrderRDto delivery(String number, DeliveryStatus status, String at) {
        return new DeliveryOrderRDto(
                UUID.randomUUID(),
                number,
                status,
                "Rakoto Be",
                "034 12 345 67",
                "Lot II J 45",
                "Ivandry",
                UUID.randomUUID(),
                TODAY,
                1,
                new BigDecimal("8500"),
                null,
                null,
                null,
                null,
                List.of(),
                Instant.parse(at),
                null,
                null,
                null,
                Instant.parse(at),
                0L);
    }
}
