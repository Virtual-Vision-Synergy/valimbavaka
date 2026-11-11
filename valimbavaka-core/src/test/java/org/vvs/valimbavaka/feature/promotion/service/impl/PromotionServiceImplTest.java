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

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.promotion.domain.Promotion;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionBasketLine;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDetails;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionDiscount;
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
    void recordUsage_limitNotReached_incrementsTheCounter() {
        // Arrange
        var promotionId = UUID.randomUUID();
        when(promotionRepository.incrementUsage(promotionId)).thenReturn(1);

        // Act
        sut.recordUsage(promotionId);

        // Assert
        verify(promotionRepository).incrementUsage(promotionId);
        verify(promotionRepository, never()).findById(any());
    }

    @Test
    void recordUsage_limitReached_throwsConflict() {
        // Arrange : the atomic UPDATE changed no row, the promotion exists
        var promotionId = UUID.randomUUID();
        when(promotionRepository.incrementUsage(promotionId)).thenReturn(0);
        var existing = automatic("Octobre rose", PromotionKind.PERCENT, "15", CHANNEL_ID);
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> sut.recordUsage(promotionId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("plafond");
    }

    @Test
    void recordUsage_unknownPromotion_throwsNotFound() {
        // Arrange
        var promotionId = UUID.randomUUID();
        when(promotionRepository.incrementUsage(promotionId)).thenReturn(0);
        when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.recordUsage(promotionId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void releaseUsage_anyPromotion_decrementsTheCounter() {
        // Arrange
        var promotionId = UUID.randomUUID();

        // Act
        sut.releaseUsage(promotionId);

        // Assert
        verify(promotionRepository).decrementUsage(promotionId);
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

    @Test
    void getAutomaticPromotions_readsTheAutomaticPromotionsActiveOnTheBusinessDay() {
        // Arrange
        var running = automatic("Octobre rose", PromotionKind.PERCENT, "15", CHANNEL_ID);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(running));

        // Act
        sut.getAutomaticPromotions();

        // Assert
        verify(promotionMapper).toRDto(running, PromotionStatus.ACTIVE);
    }

    @Test
    void priceBasket_twoPromotionsReachTheBasket_keepsTheOneThatGivesTheLargestDiscount() {
        // Arrange : 17 000 Ar of soap, 15 % = 2 550 Ar against 1 000 Ar off
        var percent = automatic("Octobre rose", PromotionKind.PERCENT, "15", CHANNEL_ID);
        var amount = automatic("Mille ariary", PromotionKind.AMOUNT, "1000", CHANNEL_ID);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(amount, percent));

        // Act
        var discount = sut.priceBasket(CHANNEL_ID, List.of(soap("8500", "2")));

        // Assert
        assertThat(discount.promotionName()).isEqualTo("Octobre rose");
        assertThat(discount.amount()).isEqualByComparingTo("2550");
    }

    @Test
    void priceBasket_sameDiscount_keepsTheFirstOfTheRepositoryOrder() {
        // Arrange : closest end first, so the first one wins a tie
        var first = automatic("Premiere", PromotionKind.AMOUNT, "1000", CHANNEL_ID);
        var second = automatic("Seconde", PromotionKind.AMOUNT, "1000", CHANNEL_ID);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(first, second));

        // Act
        var discount = sut.priceBasket(CHANNEL_ID, List.of(soap("8500", "2")));

        // Assert
        assertThat(discount.promotionName()).isEqualTo("Premiere");
    }

    @Test
    void priceBasket_promotionOfAnotherChannel_isIgnored() {
        // Arrange
        var otherChannel = UUID.randomUUID();
        var facebookOnly = automatic("Facebook", PromotionKind.PERCENT, "50", otherChannel);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(facebookOnly));

        // Act
        var discount = sut.priceBasket(CHANNEL_ID, List.of(soap("8500", "2")));

        // Assert
        assertThat(discount).isEqualTo(PromotionDiscount.NONE);
    }

    @Test
    void priceBasket_noPromotionReachesTheBasket_returnsNone() {
        // Arrange : buy 3, one free, with 2 bought
        var buyThree = automatic("Trois pour deux", PromotionKind.BUY_X, "3", CHANNEL_ID);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(buyThree));

        // Act
        var discount = sut.priceBasket(CHANNEL_ID, List.of(soap("8500", "2")));

        // Assert
        assertThat(discount).isEqualTo(PromotionDiscount.NONE);
    }

    @Test
    void priceBasket_emptyBasket_returnsNoneWithoutReadingThePromotions() {
        // Act
        var discount = sut.priceBasket(CHANNEL_ID, List.of());

        // Assert
        assertThat(discount).isEqualTo(PromotionDiscount.NONE);
        verify(promotionRepository, never()).findAutomaticActive(any());
    }

    @Test
    void getPromotionByCode_codeInLowerCaseWithSpaces_looksUpTheUpperCaseCode() {
        // Arrange
        var promo = coded("Code Octobre", "20", CHANNEL_ID, period(0, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act
        sut.getPromotionByCode("  octo20 ", CHANNEL_ID);

        // Assert
        verify(promotionMapper).toRDto(promo, PromotionStatus.ACTIVE);
    }

    @Test
    void getPromotionByCode_lastDayOfThePeriod_isStillValid() {
        // Arrange : the period ends today in Antananarivo
        var promo = coded("Code Octobre", "20", CHANNEL_ID, period(-5, 0), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act
        sut.getPromotionByCode("OCTO20", CHANNEL_ID);

        // Assert
        verify(promotionMapper).toRDto(promo, PromotionStatus.ACTIVE);
    }

    @Test
    void getPromotionByCode_unknownCode_throwsNotFound() {
        // Arrange
        when(promotionRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getPromotionByCode("nope", CHANNEL_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPromotionByCode_periodNotStarted_throwsBusinessException() {
        // Arrange
        var promo = coded("Code Octobre", "20", CHANNEL_ID, period(1, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act + Assert
        assertThatThrownBy(() -> sut.getPromotionByCode("OCTO20", CHANNEL_ID))
                .isExactlyInstanceOf(BusinessException.class)
                .hasMessageContaining("pas encore");
    }

    @Test
    void getPromotionByCode_periodEnded_throwsBusinessException() {
        // Arrange : ended yesterday
        var promo = coded("Code Octobre", "20", CHANNEL_ID, period(-30, -1), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act + Assert
        assertThatThrownBy(() -> sut.getPromotionByCode("OCTO20", CHANNEL_ID))
                .isExactlyInstanceOf(BusinessException.class)
                .hasMessageContaining("expiré");
    }

    @Test
    void getPromotionByCode_otherChannel_throwsBusinessException() {
        // Arrange
        var promo = coded("Code Facebook", "20", UUID.randomUUID(), period(0, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act + Assert
        assertThatThrownBy(() -> sut.getPromotionByCode("OCTO20", CHANNEL_ID))
                .isExactlyInstanceOf(BusinessException.class)
                .hasMessageContaining("canal");
    }

    @Test
    void getPromotionByCode_usageLimitReached_throwsBusinessException() {
        // Arrange : a limit of 0 use is reached from the start
        var promo = coded("Code Octobre", "20", CHANNEL_ID, period(0, 30), 0);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act + Assert
        assertThatThrownBy(() -> sut.getPromotionByCode("OCTO20", CHANNEL_ID))
                .isExactlyInstanceOf(BusinessException.class)
                .hasMessageContaining("plafond");
    }

    @Test
    void priceBasket_codeGivesMoreThanTheAutomaticPromotion_returnsTheCode() {
        // Arrange : 17 000 Ar of soap, code 20 % = 3 400 Ar against 15 % = 2 550 Ar
        var promo = coded("Code Octobre", "20", CHANNEL_ID, period(0, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));
        var automatic = automatic("Octobre rose", PromotionKind.PERCENT, "15", CHANNEL_ID);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(automatic));

        // Act
        var discount = sut.priceBasket(CHANNEL_ID, "octo20", List.of(soap("8500", "2")));

        // Assert
        assertThat(discount.promotionName()).isEqualTo("Code Octobre");
        assertThat(discount.amount()).isEqualByComparingTo("3400");
    }

    @Test
    void priceBasket_automaticPromotionGivesMoreThanTheCode_returnsTheAutomaticPromotion() {
        // Arrange : 17 000 Ar of soap, code 10 % = 1 700 Ar against 15 % = 2 550 Ar
        var promo = coded("Code Octobre", "10", CHANNEL_ID, period(0, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));
        var automatic = automatic("Octobre rose", PromotionKind.PERCENT, "15", CHANNEL_ID);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(automatic));

        // Act
        var discount = sut.priceBasket(CHANNEL_ID, "OCTO20", List.of(soap("8500", "2")));

        // Assert : no accumulation
        assertThat(discount.promotionName()).isEqualTo("Octobre rose");
        assertThat(discount.amount()).isEqualByComparingTo("2550");
    }

    @Test
    void priceBasket_codeAndAutomaticPromotionTie_returnsTheCode() {
        // Arrange : both give 1 700 Ar
        var promo = coded("Code Octobre", "10", CHANNEL_ID, period(0, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));
        var automatic = automatic("Dix pour cent", PromotionKind.PERCENT, "10", CHANNEL_ID);
        when(promotionRepository.findAutomaticActive(BUSINESS_TODAY)).thenReturn(List.of(automatic));

        // Act
        var discount = sut.priceBasket(CHANNEL_ID, "OCTO20", List.of(soap("8500", "2")));

        // Assert
        assertThat(discount.promotionName()).isEqualTo("Code Octobre");
    }

    @Test
    void priceBasket_codeWithoutEffectOnTheBasket_throwsBusinessException() {
        // Arrange : buy 3, one free, with 2 bought
        var promo = coded("Trois pour deux", PromotionKind.BUY_X, "3", CHANNEL_ID, period(0, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act + Assert
        assertThatThrownBy(() -> sut.priceBasket(CHANNEL_ID, "OCTO20", List.of(soap("8500", "2"))))
                .isExactlyInstanceOf(BusinessException.class)
                .hasMessageContaining("aucune remise");
    }

    @Test
    void priceBasket_codeAndEmptyBasket_throwsBusinessException() {
        // Arrange
        var promo = coded("Code Octobre", "20", CHANNEL_ID, period(0, 30), null);
        when(promotionRepository.findByCode("OCTO20")).thenReturn(Optional.of(promo));

        // Act + Assert
        assertThatThrownBy(() -> sut.priceBasket(CHANNEL_ID, "OCTO20", List.of()))
                .isExactlyInstanceOf(BusinessException.class);
    }

    @Test
    void priceBasket_unknownCode_throwsNotFound() {
        // Arrange
        when(promotionRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.priceBasket(CHANNEL_ID, "NOPE", List.of(soap("8500", "2"))))
                .isInstanceOf(NotFoundException.class);
    }

    private static PromotionPeriod period(int fromDays, int toDays) {
        return new PromotionPeriod(BUSINESS_TODAY.plusDays(fromDays), BUSINESS_TODAY.plusDays(toDays));
    }

    private static Promotion coded(
            String name, String percent, UUID channelId, PromotionPeriod period, Integer usageLimit) {
        return coded(name, PromotionKind.PERCENT, percent, channelId, period, usageLimit);
    }

    private static Promotion coded(
            String name, PromotionKind kind, String value, UUID channelId, PromotionPeriod period, Integer usageLimit) {
        return Promotion.create(new PromotionDetails(
                name,
                null,
                kind,
                new BigDecimal(value),
                "octo20",
                PromotionScope.ALL,
                Set.of(),
                period,
                Set.of(channelId),
                usageLimit));
    }

    private static PromotionBasketLine soap(String unitPrice, String quantity) {
        return new PromotionBasketLine(UUID.randomUUID(), null, new BigDecimal(unitPrice), new BigDecimal(quantity));
    }

    private static Promotion automatic(String name, PromotionKind kind, String value, UUID channelId) {
        return Promotion.create(new PromotionDetails(
                name,
                null,
                kind,
                new BigDecimal(value),
                null,
                PromotionScope.ALL,
                Set.of(),
                new PromotionPeriod(BUSINESS_TODAY, BUSINESS_TODAY.plusDays(30)),
                Set.of(channelId),
                null));
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
