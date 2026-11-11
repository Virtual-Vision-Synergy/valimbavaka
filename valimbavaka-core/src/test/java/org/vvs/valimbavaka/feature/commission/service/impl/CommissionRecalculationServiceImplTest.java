package org.vvs.valimbavaka.feature.commission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.commission.domain.Commission;
import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;
import org.vvs.valimbavaka.feature.commission.domain.CommissionRateSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.domain.CommissionStatus;
import org.vvs.valimbavaka.feature.commission.domain.CommissionTerms;
import org.vvs.valimbavaka.feature.commission.dto.ApplicableCommissionRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRecalculationWDto;
import org.vvs.valimbavaka.feature.commission.repository.CommissionRepository;
import org.vvs.valimbavaka.feature.commission.service.CommissionRateResolver;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.service.SaleService;

/** COM-05: recalculation of the computed commissions of a period, with the rate of the calculation (COM-02). */
@ExtendWith(MockitoExtension.class)
class CommissionRecalculationServiceImplTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private CommissionRateResolver rateResolver;

    @Mock
    private SaleService saleService;

    @Mock
    private JournalService journalService;

    private CommissionRecalculationServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CommissionRecalculationServiceImpl(commissionRepository, rateResolver, saleService, journalService);
    }

    @Test
    void recalculate_rateChanged_updatesTheCommissionAndTracesTheJournal() {
        // Arrange : 25 500 Ar calculés à 4 %, le taux de la vendeuse est maintenant de 5 %
        var commission = commission(CommissionSourceType.SALE, "VTE-0042", "25500.00", "4");
        givenPending(commission);
        givenRate("5", CommissionRateSource.SELLER);
        when(saleService.getPurchaseCost(commission.getSourceId())).thenReturn(new BigDecimal("1000.00"));

        // Act
        var result = sut.recalculate(new CommissionRecalculationWDto(FROM, TO, SELLER_ID));

        // Assert
        assertThat(commission.getAmount()).isEqualTo(new BigDecimal("1275.00"));
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.unchangedCount()).isZero();
        assertThat(result.previousAmount()).isEqualTo(new BigDecimal("1020.00"));
        assertThat(result.newAmount()).isEqualTo(new BigDecimal("1275.00"));
        verify(journalService)
                .record(
                        eq(JournalAction.UPDATE),
                        eq("COMMISSION"),
                        any(),
                        contains("VTE-0042 · Miora Rakoto recalculée : 1020.00 → 1275.00 Ar (taux 4.00 % → 5.00 %)"));
    }

    @Test
    void recalculate_nothingChanged_writesNothingToTheJournal() {
        // Arrange : le taux en vigueur est celui déjà appliqué (deuxième lancement)
        var commission = commission(CommissionSourceType.SALE, "VTE-0042", "25500.00", "4");
        givenPending(commission);
        givenRate("4", CommissionRateSource.SELLER);
        when(saleService.getPurchaseCost(commission.getSourceId())).thenReturn(BigDecimal.ZERO);

        // Act
        var result = sut.recalculate(new CommissionRecalculationWDto(FROM, TO, SELLER_ID));

        // Assert
        assertThat(result.updatedCount()).isZero();
        assertThat(result.unchangedCount()).isEqualTo(1);
        assertThat(result.newAmount()).isEqualTo(result.previousAmount());
        verifyNoInteractions(journalService);
    }

    @Test
    void recalculate_sameSellerAndChannel_asksTheResolverOnce() {
        // Arrange : deux ventes de la même vendeuse sur le même canal
        var first = commission(CommissionSourceType.SALE, "VTE-0001", "10000.00", "4");
        var second = commission(CommissionSourceType.SALE, "VTE-0002", "20000.00", "4");
        givenPending(first, second);
        givenRate("5", CommissionRateSource.SELLER);
        when(saleService.getPurchaseCost(any())).thenReturn(BigDecimal.ZERO);

        // Act
        var result = sut.recalculate(new CommissionRecalculationWDto(FROM, TO, null));

        // Assert
        assertThat(result.updatedCount()).isEqualTo(2);
        assertThat(result.newAmount()).isEqualTo(new BigDecimal("1500.00"));
        verify(rateResolver, times(1)).resolve(SELLER_ID, CHANNEL_ID, null);
    }

    @Test
    void recalculate_salonService_doesNotReadAPurchaseCost() {
        // Arrange
        var service = commission(CommissionSourceType.SERVICE_RECORD, "PRE-0007", "30000.00", "4");
        givenPending(service);
        givenRate("5", CommissionRateSource.SELLER);

        // Act
        var result = sut.recalculate(new CommissionRecalculationWDto(FROM, TO, SELLER_ID));

        // Assert
        assertThat(result.updatedCount()).isEqualTo(1);
        verify(saleService, never()).getPurchaseCost(any());
    }

    @Test
    void recalculate_validatedCommissions_areCountedButNeverRead() {
        // Arrange : aucune commission à recalculer, deux déjà validées
        givenPending();
        when(commissionRepository.countByStatusInPeriod(CommissionStatus.VALIDATED, SELLER_ID, FROM, TO))
                .thenReturn(2L);

        // Act
        var result = sut.recalculate(new CommissionRecalculationWDto(FROM, TO, SELLER_ID));

        // Assert
        assertThat(result.validatedCount()).isEqualTo(2);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.previousAmount()).isEqualTo(BigDecimal.ZERO);
        verifyNoInteractions(rateResolver, saleService, journalService);
    }

    @Test
    void recalculate_periodInverted_throwsBusinessException() {
        // Arrange
        var request = new CommissionRecalculationWDto(TO, FROM, null);

        // Act + Assert
        assertThatThrownBy(() -> sut.recalculate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("précéder");
        verifyNoInteractions(commissionRepository);
    }

    @Test
    void recalculate_periodLongerThanAYear_throwsBusinessException() {
        // Arrange : 366 jours d'écart, soit plus d'un an
        var request = new CommissionRecalculationWDto(FROM, FROM.plusDays(366), null);

        // Act + Assert
        assertThatThrownBy(() -> sut.recalculate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("un an");
        verifyNoInteractions(commissionRepository);
    }

    @Test
    void recalculate_periodOfExactlyAYear_isAccepted() {
        // Arrange : 365 jours d'écart, la limite acceptée
        var request = new CommissionRecalculationWDto(FROM, FROM.plusDays(365), null);
        when(commissionRepository.findByStatusInPeriod(any(), any(), any(), any()))
                .thenReturn(List.of());

        // Act
        var result = sut.recalculate(request);

        // Assert
        assertThat(result.updatedCount()).isZero();
    }

    private void givenPending(Commission... commissions) {
        when(commissionRepository.findByStatusInPeriod(eq(CommissionStatus.CALCULATED), any(), eq(FROM), eq(TO)))
                .thenReturn(List.of(commissions));
    }

    private void givenRate(String rate, CommissionRateSource source) {
        when(rateResolver.resolve(SELLER_ID, CHANNEL_ID, null))
                .thenReturn(new ApplicableCommissionRDto(new BigDecimal(rate), CommissionBase.REVENUE, source, null));
    }

    private static Commission commission(CommissionSourceType type, String number, String revenue, String rate) {
        var source = new CommissionSource(
                type,
                UUID.randomUUID(),
                number,
                SELLER_ID,
                CHANNEL_ID,
                null,
                FROM,
                new BigDecimal(revenue),
                purchaseCost(type));
        var terms =
                new CommissionTerms(new BigDecimal(rate), CommissionBase.REVENUE, CommissionRateSource.SELLER, null);
        return Commission.calculate(source, "Miora Rakoto", terms);
    }

    private static @Nullable BigDecimal purchaseCost(CommissionSourceType type) {
        return type == CommissionSourceType.SALE ? BigDecimal.ZERO : null;
    }
}
