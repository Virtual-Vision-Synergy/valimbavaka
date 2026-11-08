package org.vvs.valimbavaka.feature.promotion.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionPeriod;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionStatus;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionWDto;
import org.vvs.valimbavaka.feature.promotion.mapper.PromotionMapper;
import org.vvs.valimbavaka.feature.promotion.repository.PromotionRepository;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    /** 2026-11-07 at 22:00 UTC is already 2026-11-08 at 01:00 in Antananarivo. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T22:00:00Z"), ZoneOffset.UTC);

    private static final LocalDate BUSINESS_TODAY = LocalDate.of(2026, 11, 8);
    private static final UUID CHANNEL_ID = UUID.randomUUID();

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionMapper promotionMapper;

    @Mock
    private PromotionRules promotionRules;

    @Mock
    private JournalService journalService;

    private PromotionServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new PromotionServiceImpl(promotionRepository, promotionMapper, promotionRules, journalService, CLOCK);
    }

    @Test
    void createPromotion_validRequest_savesNormalizedPromotionAndTracesInJournal() {
        // Arrange
        when(promotionRepository.existsByCode("ROSE15")).thenReturn(false);
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = request(" rose15 ", List.of(CHANNEL_ID, CHANNEL_ID));

        // Act
        sut.createPromotion(request);

        // Assert
        var saved = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Octobre rose");
        assertThat(saved.getValue().getCode()).isEqualTo("ROSE15");
        assertThat(saved.getValue().getDescription()).isNull();
        assertThat(saved.getValue().getChannelIds()).containsExactly(CHANNEL_ID);
        assertThat(saved.getValue().getTargetIds()).isEmpty();
        assertThat(saved.getValue().getUsageCount()).isZero();
        verify(promotionRules).check(any(PromotionDetails.class), eq(BUSINESS_TODAY));
        verify(journalService)
                .record(eq(JournalAction.CREATE), eq("PROMOTION"), any(), eq("Création de la promotion Octobre rose"));
    }

    @Test
    void createPromotion_withoutCode_savesAutomaticPromotionWithoutCodeLookup() {
        // Arrange
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sut.createPromotion(request("  ", List.of(CHANNEL_ID)));

        // Assert
        verify(promotionRepository, never()).existsByCode(anyString());
        var saved = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(saved.capture());
        assertThat(saved.getValue().getCode()).isNull();
    }

    @Test
    void createPromotion_codeAlreadyUsed_throwsConflictWithoutSaving() {
        // Arrange
        when(promotionRepository.existsByCode("ROSE15")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.createPromotion(request("ROSE15", List.of(CHANNEL_ID))))
                .isInstanceOf(ConflictException.class);
        verify(promotionRepository, never()).save(any());
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void getPromotionById_unknownId_throwsNotFound() {
        // Arrange
        var id = UUID.randomUUID();
        when(promotionRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getPromotionById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPromotionById_periodStartingTomorrow_mapsScheduledStatus() {
        // Arrange
        var id = UUID.randomUUID();
        var promotion = Promotion.create(new PromotionDetails(
                "Black Friday",
                null,
                PromotionKind.PERCENT,
                new BigDecimal("25"),
                "BF25",
                PromotionScope.ALL,
                Set.of(),
                new PromotionPeriod(BUSINESS_TODAY.plusDays(1), BUSINESS_TODAY.plusDays(3)),
                Set.of(CHANNEL_ID),
                300));
        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));

        // Act
        sut.getPromotionById(id);

        // Assert
        verify(promotionMapper).toRDto(promotion, PromotionStatus.SCHEDULED);
    }

    @Test
    void getPromotions_activeFilter_searchesByStatusNameOnTheBusinessDayWithWhiteListedSort() {
        // Arrange
        var promotion = promotion(new PromotionPeriod(BUSINESS_TODAY.minusDays(5), BUSINESS_TODAY));
        when(promotionRepository.search(eq("rose"), eq("ACTIVE"), eq(BUSINESS_TODAY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(promotion)));
        var query = new PageQuery(2, 10, "  rose ", "name", SortDirection.ASC);

        // Act
        var page = sut.getPromotions(query, PromotionStatus.ACTIVE);

        // Assert
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(promotionRepository).search(eq("rose"), eq("ACTIVE"), eq(BUSINESS_TODAY), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("name")).isNotNull();
        assertThat(page.totalCount()).isEqualTo(1);
        verify(promotionMapper).toRDto(promotion, PromotionStatus.ACTIVE);
    }

    @Test
    void getPromotions_noStatusAndSortOutsideWhiteList_searchesAllByStartDateDescending() {
        // Arrange
        var ended = promotion(new PromotionPeriod(BUSINESS_TODAY.minusDays(10), BUSINESS_TODAY.minusDays(1)));
        when(promotionRepository.search(eq(""), eq(PromotionRepository.ANY_STATUS), eq(BUSINESS_TODAY), any()))
                .thenReturn(new PageImpl<>(List.of(ended)));

        // Act
        sut.getPromotions(new PageQuery(null, null, null, "code; drop table promotions", null), null);

        // Assert
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(promotionRepository).search(eq(""), eq("ALL"), eq(BUSINESS_TODAY), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("startsOn")).isEqualTo(Sort.Order.desc("startsOn"));
        verify(promotionMapper).toRDto(ended, PromotionStatus.ENDED);
    }

    @Test
    void getActivePromotionsFor_articleWithRange_looksForTheArticleItsRangeAndTheCatalogueOnTheBusinessDay() {
        // Arrange
        var articleId = UUID.randomUUID();
        var rangeId = UUID.randomUUID();
        var running = promotion(new PromotionPeriod(BUSINESS_TODAY, BUSINESS_TODAY));
        when(promotionRepository.findActiveTargeting(Set.of(articleId, rangeId), PromotionScope.ALL, BUSINESS_TODAY))
                .thenReturn(List.of(running));

        // Act
        var promotions = sut.getActivePromotionsFor(articleId, rangeId);

        // Assert
        assertThat(promotions).hasSize(1);
        verify(promotionMapper).toRDto(running, PromotionStatus.ACTIVE);
    }

    @Test
    void getActivePromotionsFor_articleWithoutRange_looksForTheArticleOnly() {
        // Arrange
        var articleId = UUID.randomUUID();

        // Act
        var promotions = sut.getActivePromotionsFor(articleId, null);

        // Assert
        assertThat(promotions).isEmpty();
        verify(promotionRepository).findActiveTargeting(Set.of(articleId), PromotionScope.ALL, BUSINESS_TODAY);
    }

    private static Promotion promotion(PromotionPeriod period) {
        return Promotion.create(new PromotionDetails(
                "Octobre rose",
                null,
                PromotionKind.PERCENT,
                new BigDecimal("15"),
                null,
                PromotionScope.ALL,
                Set.of(),
                period,
                Set.of(CHANNEL_ID),
                null));
    }

    private static PromotionWDto request(String code, List<UUID> channelIds) {
        return new PromotionWDto(
                "  Octobre rose ",
                " ",
                PromotionKind.PERCENT,
                new BigDecimal("15"),
                code,
                PromotionScope.ALL,
                null,
                BUSINESS_TODAY,
                BUSINESS_TODAY.plusDays(30),
                channelIds,
                null);
    }
}
