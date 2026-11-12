package org.vvs.valimbavaka.feature.dayclosing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
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
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLocationRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingSalonLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonConsumptionRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;

@ExtendWith(MockitoExtension.class)
class DayClosingSalonServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static final Instant START_OF_TODAY = Instant.parse("2026-11-10T21:00:00Z");
    private static final Instant START_OF_TOMORROW = Instant.parse("2026-11-11T21:00:00Z");

    @Mock
    private DayClosingService dayClosingService;

    @Mock
    private ServiceRecordService serviceRecordService;

    @Mock
    private ArticleService articleService;

    private DayClosingSalonServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DayClosingSalonServiceImpl(dayClosingService, serviceRecordService, articleService);
    }

    @Test
    void getSalonConsumption_dayNotOpened_propagatesTheConflictWithoutReadingTheServices() {
        // Arrange
        when(dayClosingService.getClosing()).thenThrow(new ConflictException("La journée n'est pas ouverte."));

        // Act + Assert
        assertThatThrownBy(() -> sut.getSalonConsumption()).isInstanceOf(ConflictException.class);
        verifyNoInteractions(serviceRecordService, articleService);
    }

    @Test
    void getSalonConsumption_salonColumnEqualsTheServices_matchesAndTotalsAreEqual() {
        // Arrange
        var oil = article("HUI-01", "Huile");
        var gel = article("GEL-01", "Gel");
        stubClosing(salonLine(oil, "0.035"), salonLine(gel, "2"));
        stubServices(List.of(consumed(oil, "0.035"), consumed(gel, "2")), 3);
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of(oil, gel));

        // Act
        var salon = sut.getSalonConsumption();

        // Assert
        assertThat(salon.matches()).isTrue();
        assertThat(salon.servicesCount()).isEqualTo(3);
        assertThat(salon.totalConsumed()).isEqualByComparingTo("2.035");
        assertThat(salon.totalInClosing()).isEqualByComparingTo(salon.totalConsumed());
        assertThat(salon.lines()).extracting(ClosingSalonLineRDto::reference).containsExactly("GEL-01", "HUI-01");
        assertThat(salon.lines()).allSatisfy(line -> assertThat(line.gap()).isZero());
    }

    @Test
    void getSalonConsumption_salonExitWithoutService_showsAPositiveGapAndDoesNotMatch() {
        // Arrange
        var gel = article("GEL-01", "Gel");
        stubClosing(salonLine(gel, "5"));
        stubServices(List.of(consumed(gel, "3")), 1);
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of(gel));

        // Act
        var salon = sut.getSalonConsumption();

        // Assert
        assertThat(salon.matches()).isFalse();
        assertThat(salon.totalConsumed()).isEqualByComparingTo("3");
        assertThat(salon.totalInClosing()).isEqualByComparingTo("5");
        assertThat(salon.lines().getFirst().gap()).isEqualByComparingTo("2");
    }

    @Test
    void getSalonConsumption_serviceWithoutClosingLine_showsANegativeGap() {
        // Arrange: the closing holds no salon quantity for the article, the service consumed some
        var oil = article("HUI-01", "Huile");
        stubClosing();
        stubServices(List.of(consumed(oil, "1")), 1);
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of(oil));

        // Act
        var salon = sut.getSalonConsumption();

        // Assert
        assertThat(salon.matches()).isFalse();
        assertThat(salon.lines().getFirst().inClosing()).isZero();
        assertThat(salon.lines().getFirst().gap()).isEqualByComparingTo("-1");
    }

    @Test
    void getSalonConsumption_noServiceAndNoSalonExit_matchesWithZeroTotals() {
        // Arrange
        stubClosing();
        stubServices(List.of(), 0);
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of());

        // Act
        var salon = sut.getSalonConsumption();

        // Assert
        assertThat(salon.matches()).isTrue();
        assertThat(salon.lines()).isEmpty();
        assertThat(salon.totalConsumed()).isZero();
        assertThat(salon.totalInClosing()).isZero();
    }

    @Test
    void getSalonConsumption_dayOfMadagascar_readsTheServicesFromMidnightToMidnightLocalTime() {
        // Arrange
        stubClosing();
        stubServices(List.of(), 0);
        when(articleService.getArticlesByIds(anyCollection())).thenReturn(List.of());

        // Act
        sut.getSalonConsumption();

        // Assert
        verify(serviceRecordService).getSalonConsumption(START_OF_TODAY, START_OF_TOMORROW);
        verify(serviceRecordService).getServiceRecordsBetween(START_OF_TODAY, START_OF_TOMORROW);
    }

    private void stubClosing(ClosingLineRDto... salonLines) {
        var salon = new ClosingLocationRDto(UUID.randomUUID(), "TRADE_SHOW", "Salon", List.of(salonLines));
        var closing = new DayClosingRDto(UUID.randomUUID(), TODAY, DayStatus.OPEN, List.of(salon));
        when(dayClosingService.getClosing()).thenReturn(closing);
    }

    private void stubServices(List<SalonConsumptionRDto> consumption, int servicesCount) {
        when(serviceRecordService.getSalonConsumption(START_OF_TODAY, START_OF_TOMORROW))
                .thenReturn(consumption);
        var records = Collections.nCopies(servicesCount, mock(ServiceRecordRDto.class));
        when(serviceRecordService.getServiceRecordsBetween(START_OF_TODAY, START_OF_TOMORROW))
                .thenReturn(records);
    }

    private static SalonConsumptionRDto consumed(ArticleRDto article, String quantity) {
        return new SalonConsumptionRDto(article.id(), new BigDecimal(quantity));
    }

    private static ClosingLineRDto salonLine(ArticleRDto article, String salon) {
        var quantity = new BigDecimal(salon);
        return new ClosingLineRDto(
                article.id(),
                article.code(),
                article.name(),
                quantity,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                quantity,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null);
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
}
