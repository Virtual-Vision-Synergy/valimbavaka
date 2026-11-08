package org.vvs.valimbavaka.feature.shooting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;
import org.vvs.valimbavaka.feature.shooting.repository.ShootingArticleQuantity;
import org.vvs.valimbavaka.feature.shooting.repository.ShootingRepository;

@ExtendWith(MockitoExtension.class)
class ShootingSummaryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-08T15:00:00Z");
    private static final UUID SOAP_ID = UUID.randomUUID();
    private static final UUID SCRUB_ID = UUID.randomUUID();

    @Mock
    private ShootingRepository shootingRepository;

    @Mock
    private ArticleService articleService;

    private ShootingSummaryServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ShootingSummaryServiceImpl(shootingRepository, articleService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getSummary_exitsOut_countsOutAndLateExitsAndValuesTheQuantitiesAtCost() {
        // Arrange
        when(shootingRepository.countByStatus(ShootingStatus.OUT)).thenReturn(3L);
        when(shootingRepository.countByStatusAndExpectedReturnAtBefore(ShootingStatus.OUT, NOW))
                .thenReturn(1L);
        var quantities = List.of(
                new ShootingArticleQuantity(SOAP_ID, new BigDecimal("2")),
                new ShootingArticleQuantity(SCRUB_ID, new BigDecimal("1.5")));
        when(shootingRepository.quantitiesByArticle(ShootingStatus.OUT)).thenReturn(quantities);
        var costs = Map.of(SOAP_ID, new BigDecimal("3120.50"), SCRUB_ID, new BigDecimal("4000.333"));
        when(articleService.getCosts(Set.of(SOAP_ID, SCRUB_ID))).thenReturn(costs);

        // Act
        var summary = sut.getSummary();

        // Assert
        assertThat(summary.outCount()).isEqualTo(3);
        assertThat(summary.lateCount()).isEqualTo(1);
        assertThat(summary.immobilizedValue()).isEqualTo(new BigDecimal("12241.50"));
        assertThat(summary.unvaluedQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void getSummary_articleWithoutCost_isCountedAsUnvaluedQuantity() {
        // Arrange
        when(shootingRepository.countByStatus(ShootingStatus.OUT)).thenReturn(2L);
        var quantities = List.of(
                new ShootingArticleQuantity(SOAP_ID, new BigDecimal("2")),
                new ShootingArticleQuantity(SCRUB_ID, new BigDecimal("2.5")));
        when(shootingRepository.quantitiesByArticle(ShootingStatus.OUT)).thenReturn(quantities);
        when(articleService.getCosts(Set.of(SOAP_ID, SCRUB_ID))).thenReturn(Map.of(SOAP_ID, new BigDecimal("1000")));

        // Act
        var summary = sut.getSummary();

        // Assert
        assertThat(summary.immobilizedValue()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(summary.unvaluedQuantity()).isEqualByComparingTo("2.5");
    }

    @Test
    void getSummary_noExitOut_returnsZerosWithoutReadingTheCosts() {
        // Arrange
        when(shootingRepository.quantitiesByArticle(ShootingStatus.OUT)).thenReturn(List.of());

        // Act
        var summary = sut.getSummary();

        // Assert
        assertThat(summary.outCount()).isZero();
        assertThat(summary.lateCount()).isZero();
        assertThat(summary.immobilizedValue()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.unvaluedQuantity()).isEqualByComparingTo("0");
        verifyNoInteractions(articleService);
    }
}
