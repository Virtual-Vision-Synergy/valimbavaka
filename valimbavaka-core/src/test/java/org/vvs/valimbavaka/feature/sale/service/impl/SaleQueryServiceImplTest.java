package org.vvs.valimbavaka.feature.sale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.vvs.valimbavaka.feature.sale.domain.SaleFilter;
import org.vvs.valimbavaka.feature.sale.repository.SaleQueryRepository;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@ExtendWith(MockitoExtension.class)
class SaleQueryServiceImplTest {

    /** 21:30 UTC on 7 November = 00:30 on 8 November in Madagascar (UTC+3). */
    private static final Clock JUST_AFTER_MIDNIGHT = Clock.fixed(Instant.parse("2026-11-07T21:30:00Z"), ZoneOffset.UTC);

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID COLLEAGUE = UUID.randomUUID();
    private static final UUID FACEBOOK = UUID.randomUUID();
    private static final PageQuery FIRST_PAGE = new PageQuery(1, 20, null, null, null);

    @Mock
    private SaleQueryRepository saleQueryRepository;

    @Mock
    private SaleSummaryAssembler summaryAssembler;

    @Mock
    private UserService userService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    @Captor
    private ArgumentCaptor<Pageable> pageable;

    private SaleQueryServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SaleQueryServiceImpl(
                saleQueryRepository, summaryAssembler, userService, auditorAware, JUST_AFTER_MIDNIGHT);
    }

    @Test
    void getSales_withoutDate_readsTodayInMadagascarTimeAndSkipsTheLinesOfAnEmptyPage() {
        // Arrange
        connectedAs(ADMIN, false);
        whenSearchedReturn(Page.empty());

        // Act
        sut.getSales(FIRST_PAGE, new SaleFilter(null, null, null));

        // Assert
        verify(saleQueryRepository).searchIds(eq(TODAY), isNull(), isNull(), any(Pageable.class));
        verify(saleQueryRepository, never()).findWithLinesByIdIn(anyCollection());
    }

    @Test
    void getSales_administrator_keepsTheRequestedDayChannelAndSeller() {
        // Arrange
        connectedAs(ADMIN, false);
        whenSearchedReturn(Page.empty());
        var day = LocalDate.of(2026, 11, 2);

        // Act
        sut.getSales(FIRST_PAGE, new SaleFilter(day, FACEBOOK, COLLEAGUE));

        // Assert
        verify(saleQueryRepository).searchIds(eq(day), eq(FACEBOOK), eq(COLLEAGUE), any(Pageable.class));
    }

    @Test
    void getSales_activeSeller_readsOnlyTheSalesCreditedToHer() {
        // Arrange
        connectedAs(SELLER, true);
        whenSearchedReturn(Page.empty());

        // Act
        sut.getSales(FIRST_PAGE, new SaleFilter(null, FACEBOOK, COLLEAGUE));

        // Assert
        verify(saleQueryRepository).searchIds(eq(TODAY), eq(FACEBOOK), eq(SELLER), any(Pageable.class));
    }

    @Test
    void getSales_sortOutsideTheWhitelist_fallsBackToTheMostRecentFirst() {
        // Arrange
        connectedAs(ADMIN, false);
        whenSearchedReturn(Page.empty());

        // Act
        sut.getSales(new PageQuery(2, 100, null, "unitCost", null), new SaleFilter(null, null, null));

        // Assert
        verify(saleQueryRepository).searchIds(eq(TODAY), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void getSales_sortOnTheTotal_isKept() {
        // Arrange
        connectedAs(ADMIN, false);
        whenSearchedReturn(Page.empty());

        // Act
        sut.getSales(new PageQuery(1, 20, null, "total", SortDirection.ASC), new SaleFilter(null, null, null));

        // Assert
        verify(saleQueryRepository).searchIds(eq(TODAY), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "total"));
    }

    @Test
    void getSales_secondPage_returnsTheCountsAndTheRowsInThePageOrder() {
        // Arrange
        connectedAs(ADMIN, false);
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        whenSearchedReturn(new PageImpl<>(ids, PageRequest.of(1, 2), 5));
        when(saleQueryRepository.findWithLinesByIdIn(ids)).thenReturn(List.of());
        when(summaryAssembler.toRDtos(ids, List.of())).thenReturn(List.of());

        // Act
        var result = sut.getSales(new PageQuery(2, 2, null, null, null), new SaleFilter(null, null, null));

        // Assert
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.totalCount()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        verify(summaryAssembler).toRDtos(ids, List.of());
    }

    private void connectedAs(UUID userId, boolean activeSeller) {
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(userId));
        when(userService.isActiveSeller(userId)).thenReturn(activeSeller);
    }

    private void whenSearchedReturn(Page<UUID> page) {
        when(saleQueryRepository.searchIds(any(), any(), any(), any())).thenReturn(page);
    }
}
