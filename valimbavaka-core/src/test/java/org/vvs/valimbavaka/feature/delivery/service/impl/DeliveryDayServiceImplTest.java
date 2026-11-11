package org.vvs.valimbavaka.feature.delivery.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryStatusCount;

@ExtendWith(MockitoExtension.class)
class DeliveryDayServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    @Mock
    private DeliveryOrderRepository orderRepository;

    @Mock
    private DeliveryMapper deliveryMapper;

    private DeliveryDayServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DeliveryDayServiceImpl(orderRepository, deliveryMapper);
    }

    @Test
    void countByStatus_someStatusesPresent_fillsTheOthersWithZero() {
        // Arrange
        when(orderRepository.countByStatus(TODAY))
                .thenReturn(List.of(
                        new DeliveryStatusCount(DeliveryStatus.ON_ROUND, 2),
                        new DeliveryStatusCount(DeliveryStatus.DELIVERED, 5)));

        // Act
        var counts = sut.countByStatus(TODAY);

        // Assert
        assertThat(counts)
                .containsEntry(DeliveryStatus.ON_ROUND, 2L)
                .containsEntry(DeliveryStatus.DELIVERED, 5L)
                .containsEntry(DeliveryStatus.TO_PREPARE, 0L)
                .containsEntry(DeliveryStatus.FAILED, 0L)
                .containsEntry(DeliveryStatus.CANCELLED, 0L);
    }

    @Test
    void countByStatus_noOrderToday_returnsZeroForEveryStatus() {
        // Arrange
        when(orderRepository.countByStatus(TODAY)).thenReturn(List.of());

        // Act
        var counts = sut.countByStatus(TODAY);

        // Assert
        assertThat(counts).hasSize(DeliveryStatus.values().length);
        assertThat(counts.values()).containsOnly(0L);
    }

    @Test
    void getRecentOrders_limit_asksTheLatestUpdatedOrdersOfTheDay() {
        // Arrange
        var captor = ArgumentCaptor.forClass(Pageable.class);
        when(orderRepository.search(isNull(), eq(TODAY), isNull(), captor.capture()))
                .thenReturn(new PageImpl<DeliveryOrder>(List.of()));

        // Act
        var orders = sut.getRecentOrders(TODAY, 30);

        // Assert
        assertThat(orders).isEmpty();
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }
}
