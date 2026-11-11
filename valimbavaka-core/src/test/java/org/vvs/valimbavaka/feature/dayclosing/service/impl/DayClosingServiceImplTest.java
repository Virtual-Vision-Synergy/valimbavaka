package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.article.dto.ArticleRDto;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLocationRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.dto.StockBalanceRDto;
import org.vvs.valimbavaka.feature.stock.dto.StockMovementTotalRDto;
import org.vvs.valimbavaka.feature.stock.service.StockDayLedgerService;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class DayClosingServiceImplTest {

    /** 13:00 in Antananarivo (UTC+3), 2026-11-11. */
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static final Instant START_OF_TODAY = Instant.parse("2026-11-10T21:00:00Z");
    private static final Instant START_OF_TOMORROW = Instant.parse("2026-11-11T21:00:00Z");

    private static final StockLocationRDto WAREHOUSE = location("WAREHOUSE", "Entrepôt");
    private static final StockLocationRDto STORE = location("STORE", "Boutique");
    private static final StockLocationRDto DELIVERY = location("DELIVERY", "Livraison");
    private static final StockLocationRDto SALON = location("TRADE_SHOW", "Salon");

    @Mock
    private BusinessDayService businessDayService;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private StockDayLedgerService stockDayLedgerService;

    @Mock
    private ArticleService articleService;

    private DayClosingServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DayClosingServiceImpl(
                businessDayService,
                referentialService,
                stockDayLedgerService,
                articleService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getClosing_dayNotOpened_throwsConflictWithoutReadingTheStock() {
        // Arrange
        when(businessDayService.findToday()).thenReturn(null);

        // Act + Assert
        assertThatThrownBy(() -> sut.getClosing())
                .isInstanceOf(ConflictException.class)
                .hasMessage("La journée du 11/11/2026 n'est pas ouverte.");
        verifyNoInteractions(stockDayLedgerService, articleService);
    }

    @Test
    void getClosing_openDay_returnsTheThreeLocationsInDisplayOrderWithoutTheWarehouse() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubLocations(SALON, WAREHOUSE, DELIVERY, STORE);
        stubLedger(List.of(), List.of(), List.of());

        // Act
        var closing = sut.getClosing();

        // Assert
        assertThat(closing.businessDate()).isEqualTo(TODAY);
        assertThat(closing.status()).isEqualTo(DayStatus.OPEN);
        assertThat(closing.locations())
                .extracting(ClosingLocationRDto::code)
                .containsExactly("STORE", "DELIVERY", "TRADE_SHOW");
        assertThat(closing.locations())
                .allSatisfy(location -> assertThat(location.lines()).isEmpty());
    }

    @Test
    void getClosing_closedDay_stillReturnsTheReconciliation() {
        // Arrange
        stubDay(DayStatus.CLOSED);
        stubLocations(STORE);
        stubLedger(List.of(), List.of(), List.of());

        // Act
        var closing = sut.getClosing();

        // Assert
        assertThat(closing.status()).isEqualTo(DayStatus.CLOSED);
        assertThat(closing.locations()).hasSize(1);
    }

    @Test
    void getClosing_dayOfMadagascar_readsTheLedgerFromMidnightToMidnightLocalTime() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubLocations(STORE);
        stubLedger(List.of(), List.of(), List.of());

        // Act
        sut.getClosing();

        // Assert
        verify(stockDayLedgerService).getBalancesBefore(START_OF_TODAY, List.of(STORE.id()));
        verify(stockDayLedgerService).getMovementTotals(START_OF_TODAY, START_OF_TOMORROW, List.of(STORE.id()));
        verify(stockDayLedgerService).getBalancesBefore(START_OF_TOMORROW, List.of(STORE.id()));
    }

    @Test
    void getClosing_movementsOfTheDay_buildsTheSixComponentsTheTheoreticalAndTheGap() {
        // Arrange
        var soap = article("ART-0031", "Savon");
        stubDay(DayStatus.OPEN);
        stubLocations(STORE);
        stubLedger(
                List.of(balance(soap, STORE, "4")),
                List.of(
                        total(soap, STORE, StockMovementType.TRANSFER, "30"),
                        total(soap, STORE, StockMovementType.RETURN, "1"),
                        total(soap, STORE, StockMovementType.SALE, "-19"),
                        total(soap, STORE, StockMovementType.SHOOTING, "-2")),
                List.of(balance(soap, STORE, "12")));
        when(articleService.getArticlesByIds(any())).thenReturn(List.of(soap));

        // Act
        var line = sut.getClosing().locations().getFirst().lines().getFirst();

        // Assert
        assertThat(line.reference()).isEqualTo("ART-0031");
        assertThat(line.opening()).isEqualByComparingTo("4");
        assertThat(line.transferred()).isEqualByComparingTo("30");
        assertThat(line.returned()).isEqualByComparingTo("1");
        assertThat(line.sold()).isEqualByComparingTo("19");
        assertThat(line.delivered()).isZero();
        assertThat(line.salon()).isZero();
        assertThat(line.shooting()).isEqualByComparingTo("2");
        assertThat(line.theoretical()).isEqualByComparingTo("14");
        assertThat(line.systemStock()).isEqualByComparingTo("12");
        assertThat(line.gap()).isEqualByComparingTo("-2");
    }

    @Test
    void getClosing_saleInTheDeliveryLocation_isReportedAsDelivered() {
        // Arrange
        var soap = article("ART-0031", "Savon");
        stubDay(DayStatus.OPEN);
        stubLocations(STORE, DELIVERY);
        stubLedger(
                List.of(),
                List.of(
                        total(soap, DELIVERY, StockMovementType.TRANSFER, "8"),
                        total(soap, DELIVERY, StockMovementType.SALE, "-6")),
                List.of(balance(soap, DELIVERY, "2")));
        when(articleService.getArticlesByIds(any())).thenReturn(List.of(soap));

        // Act
        var closing = sut.getClosing();

        // Assert
        assertThat(closing.locations().get(0).lines()).isEmpty();
        var line = closing.locations().get(1).lines().getFirst();
        assertThat(line.delivered()).isEqualByComparingTo("6");
        assertThat(line.sold()).isZero();
        assertThat(line.theoretical()).isEqualByComparingTo("2");
    }

    @Test
    void getClosing_severalArticles_sortsTheLinesByName() {
        // Arrange
        var shampoo = article("ART-0002", "shampoing");
        var balm = article("ART-0003", "Baume");
        var oil = article("ART-0001", "Huile");
        stubDay(DayStatus.OPEN);
        stubLocations(STORE);
        stubLedger(
                List.of(),
                List.of(
                        total(shampoo, STORE, StockMovementType.TRANSFER, "1"),
                        total(oil, STORE, StockMovementType.TRANSFER, "1"),
                        total(balm, STORE, StockMovementType.TRANSFER, "1")),
                List.of());
        when(articleService.getArticlesByIds(any())).thenReturn(List.of(shampoo, balm, oil));

        // Act
        var lines = sut.getClosing().locations().getFirst().lines();

        // Assert
        assertThat(lines).extracting(ClosingLineRDto::name).containsExactly("Baume", "Huile", "shampoing");
    }

    @Test
    void getClosing_transfersCancellingOut_leavesNoLine() {
        // Arrange
        var soap = article("ART-0031", "Savon");
        stubDay(DayStatus.OPEN);
        stubLocations(STORE);
        stubLedger(
                List.of(),
                List.of(
                        total(soap, STORE, StockMovementType.TRANSFER, "5"),
                        total(soap, STORE, StockMovementType.TRANSFER, "-5")),
                List.of());
        when(articleService.getArticlesByIds(any())).thenReturn(List.of(soap));

        // Act
        var lines = sut.getClosing().locations().getFirst().lines();

        // Assert
        assertThat(lines).isEmpty();
    }

    @Test
    void getClosing_articleHeldWithoutMovementToday_showsItsOpeningStock() {
        // Arrange
        var soap = article("ART-0031", "Savon");
        stubDay(DayStatus.OPEN);
        stubLocations(STORE);
        stubLedger(List.of(balance(soap, STORE, "3")), List.of(), List.of(balance(soap, STORE, "3")));
        when(articleService.getArticlesByIds(any())).thenReturn(List.of(soap));

        // Act
        var line = sut.getClosing().locations().getFirst().lines().getFirst();

        // Assert
        assertThat(line.opening()).isEqualByComparingTo("3");
        assertThat(line.theoretical()).isEqualByComparingTo("3");
        assertThat(line.gap()).isZero();
    }

    private void stubDay(DayStatus status) {
        var day = new BusinessDayRDto(UUID.randomUUID(), TODAY, status, List.of(), NOW, null);
        when(businessDayService.findToday()).thenReturn(day);
    }

    private void stubLocations(StockLocationRDto... locations) {
        var all = new ArrayList<>(List.of(locations));
        if (all.stream().noneMatch(location -> location.code().equals("WAREHOUSE"))) {
            all.add(WAREHOUSE);
        }
        when(referentialService.getStockLocations(any())).thenReturn(new PageResponse<>(all, 1, 100, all.size(), 1));
    }

    private void stubLedger(
            List<StockBalanceRDto> opening, List<StockMovementTotalRDto> totals, List<StockBalanceRDto> stock) {
        when(stockDayLedgerService.getBalancesBefore(eq(START_OF_TODAY), anyCollection()))
                .thenReturn(opening);
        when(stockDayLedgerService.getMovementTotals(eq(START_OF_TODAY), eq(START_OF_TOMORROW), anyCollection()))
                .thenReturn(totals);
        when(stockDayLedgerService.getBalancesBefore(eq(START_OF_TOMORROW), anyCollection()))
                .thenReturn(stock);
    }

    private static StockLocationRDto location(String code, String label) {
        return new StockLocationRDto(UUID.randomUUID(), code, label);
    }

    private static ArticleRDto article(String code, String name) {
        return new ArticleRDto(
                UUID.randomUUID(),
                code,
                name,
                null,
                ArticleType.FINISHED,
                null,
                ArticleUnit.PIECE,
                null,
                null,
                BigDecimal.ZERO,
                null,
                true,
                NOW,
                NOW,
                0);
    }

    private static StockBalanceRDto balance(ArticleRDto article, StockLocationRDto location, String quantity) {
        return new StockBalanceRDto(article.id(), location.id(), new BigDecimal(quantity));
    }

    private static StockMovementTotalRDto total(
            ArticleRDto article, StockLocationRDto location, StockMovementType type, String quantity) {
        return new StockMovementTotalRDto(article.id(), location.id(), type, new BigDecimal(quantity));
    }
}
