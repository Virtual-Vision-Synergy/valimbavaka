package org.vvs.valimbavaka.feature.salonservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.salonservice.domain.SalonService;
import org.vvs.valimbavaka.feature.salonservice.domain.SalonServiceDetails;
import org.vvs.valimbavaka.feature.salonservice.repository.SalonServiceRepository;

@ExtendWith(MockitoExtension.class)
class SalonServiceCommissionRatesImplTest {

    private static final UUID SERVICE_ID = UUID.randomUUID();

    @Mock
    private SalonServiceRepository serviceRepository;

    private SalonServiceCommissionRatesImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SalonServiceCommissionRatesImpl(serviceRepository);
    }

    @Test
    void findRate_serviceWithRate_returnsItsRate() {
        // Arrange
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(new BigDecimal("12.5"))));

        // Act
        var rate = sut.findRate(SERVICE_ID);

        // Assert
        assertThat(rate).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("12.50"));
    }

    @Test
    void findRate_serviceWithoutRate_returnsEmpty() {
        // Arrange
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service(null)));

        // Act + Assert
        assertThat(sut.findRate(SERVICE_ID)).isEmpty();
    }

    @Test
    void findRate_unknownService_returnsEmpty() {
        // Arrange
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThat(sut.findRate(SERVICE_ID)).isEmpty();
    }

    private static SalonService service(BigDecimal rate) {
        return SalonService.create(
                new SalonServiceDetails("Massage huile coco", 60, new BigDecimal("40000"), rate, List.of()));
    }
}
