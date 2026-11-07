package org.vvs.valimbavaka.feature.day.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.day.dto.DayOpeningWDto;
import org.vvs.valimbavaka.feature.day.dto.DispatchLineWDto;
import org.vvs.valimbavaka.feature.day.repository.BusinessDayRepository;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.service.StockService;

@ExtendWith(MockitoExtension.class)
class DayOpeningPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final UUID WAREHOUSE = UUID.randomUUID();
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID DELIVERY = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Mock
    private BusinessDayRepository dayRepository;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private StockService stockService;

    @Mock
    private ArticleService articleService;

    private DayOpeningPolicy sut;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-11-08T21:30:00Z"), ZoneOffset.UTC);
        sut = new DayOpeningPolicy(dayRepository, referentialService, stockService, articleService, clock);
    }

    @Test
    void today_lateEveningInUtc_isTheNextDayInAntananarivo() {
        // Act
        var today = sut.today();

        // Assert: 21:30 UTC is 00:30 the next day in Antananarivo (UTC+3).
        assertThat(today).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void check_dayAlreadyOpened_throwsConflict() {
        // Arrange
        when(dayRepository.existsByBusinessDate(TODAY)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(TODAY, request()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("08/11/2026");
        verifyNoInteractions(stockService);
    }

    @Test
    void check_sameArticleTwiceForADestination_addsTheQuantitiesUpPerDestination() {
        // Arrange
        stubLocations();
        when(stockService.getQuantities(any(), any())).thenReturn(Map.of(SOAP, new BigDecimal("20")));
        var request = request(line(SOAP, STORE, "2"), line(SOAP, DELIVERY, "4"), line(SOAP, STORE, "3"));

        // Act
        var plan = sut.check(TODAY, request);

        // Assert
        assertThat(plan.warehouseId()).isEqualTo(WAREHOUSE);
        assertThat(plan.byDestination()).containsOnlyKeys(STORE, DELIVERY);
        assertThat(plan.byDestination().keySet()).containsExactly(STORE, DELIVERY);
        assertThat(plan.byDestination().get(STORE).get(SOAP)).isEqualByComparingTo("5");
        assertThat(plan.byDestination().get(DELIVERY).get(SOAP)).isEqualByComparingTo("4");
    }

    @Test
    void check_noLine_returnsAnEmptyPlanWithoutReadingTheStock() {
        // Arrange
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE);

        // Act
        var plan = sut.check(TODAY, request());

        // Assert
        assertThat(plan.byDestination()).isEmpty();
        verifyNoInteractions(stockService);
    }

    @Test
    void check_destinationIsTheWarehouse_throwsBusinessException() {
        // Arrange
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(TODAY, request(line(SOAP, WAREHOUSE, "1"))))
                .isInstanceOf(BusinessException.class)
                .isNotInstanceOf(ConflictException.class);
    }

    @Test
    void check_inactiveDestination_throwsBusinessException() {
        // Arrange
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE);
        when(referentialService.isActiveStockLocation(STORE)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.check(TODAY, request(line(SOAP, STORE, "1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("emplacement actif");
    }

    @Test
    void check_totalAcrossDestinationsAboveWarehouseStock_throwsConflictNamingTheArticle() {
        // Arrange: 8 + 5 asked, 12 at the warehouse.
        stubLocations();
        when(stockService.getQuantities(any(), any()))
                .thenReturn(Map.of(SOAP, new BigDecimal("12.000"), OIL, new BigDecimal("3")));
        when(articleService.getArticleById(SOAP)).thenReturn(article(SOAP, "Savon noir"));
        var request = request(line(SOAP, STORE, "8"), line(SOAP, DELIVERY, "5"), line(OIL, STORE, "3"));

        // Act + Assert
        assertThatThrownBy(() -> sut.check(TODAY, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Stock insuffisant à l'entrepôt pour Savon noir : 13 demandé(s), 12 disponible(s).");
    }

    private void stubLocations() {
        when(referentialService.requireActiveStockLocationId("WAREHOUSE")).thenReturn(WAREHOUSE);
        when(referentialService.isActiveStockLocation(any())).thenReturn(true);
    }

    private static DayOpeningWDto request(DispatchLineWDto... lines) {
        return new DayOpeningWDto(List.of(lines));
    }

    private static DispatchLineWDto line(UUID articleId, UUID destinationId, String quantity) {
        return new DispatchLineWDto(articleId, destinationId, new BigDecimal(quantity));
    }

    private static ArticleRDto article(UUID id, String name) {
        return new ArticleRDto(
                id,
                "ART-0001",
                name,
                null,
                ArticleType.FINISHED,
                null,
                ArticleUnit.PIECE,
                null,
                null,
                BigDecimal.TEN,
                null,
                true,
                Instant.EPOCH,
                Instant.EPOCH,
                0L);
    }
}
