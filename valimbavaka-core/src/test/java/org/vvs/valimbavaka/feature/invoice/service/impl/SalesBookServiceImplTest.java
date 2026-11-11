package org.vvs.valimbavaka.feature.invoice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.invoice.domain.SalesBookEntry;
import org.vvs.valimbavaka.feature.invoice.mapper.SalesBookMapper;
import org.vvs.valimbavaka.feature.invoice.repository.SalesBookRepository;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@ExtendWith(MockitoExtension.class)
class SalesBookServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

    @Mock
    private SalesBookRepository salesBookRepository;

    @Mock
    private SalesBookMapper salesBookMapper;

    @Captor
    private ArgumentCaptor<Pageable> pageable;

    private SalesBookServiceImpl sut;

    @BeforeEach
    void createSut() {
        sut = new SalesBookServiceImpl(salesBookRepository, salesBookMapper);
    }

    @Test
    void getSalesBook_fromAfterTo_throwsAndReadsNothing() {
        // Act + Assert
        assertThatThrownBy(() -> sut.getSalesBook(query(null, null), FROM, FROM.minusDays(1)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(salesBookRepository);
    }

    @Test
    void getSalesBook_periodOf367Days_throwsAndReadsNothing() {
        // Act + Assert
        assertThatThrownBy(() -> sut.getSalesBook(query(null, null), FROM, FROM.plusDays(366)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("366");
        verify(salesBookRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    void getSalesBook_periodOf366Days_isAccepted() {
        // Arrange
        stubEmptyPage();

        // Act
        var result = sut.getSalesBook(query(null, null), FROM, FROM.plusDays(365));

        // Assert
        assertThat(result.items()).isEmpty();
    }

    @Test
    void getSalesBook_oneDayPeriod_isAccepted() {
        // Arrange
        stubEmptyPage();

        // Act
        var result = sut.getSalesBook(query(null, null), FROM, FROM);

        // Assert
        assertThat(result.totalCount()).isZero();
        verify(salesBookRepository).search(eq(FROM), eq(FROM), eq(""), any());
    }

    @Test
    void getSalesBook_noSortGiven_ordersByDayThenIssueTimeThenNumber() {
        // Arrange
        stubEmptyPage();

        // Act
        sut.getSalesBook(query(null, null), FROM, FROM);

        // Assert
        verify(salesBookRepository).search(any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getSort().stream().map(Sort.Order::getProperty))
                .containsExactly("businessDate", "issuedAt", "number");
    }

    @Test
    void getSalesBook_unknownSortField_fallsBackToTheBusinessDay() {
        // Arrange
        stubEmptyPage();

        // Act
        sut.getSalesBook(query("issuer_nif", SortDirection.DESC), FROM, FROM);

        // Assert
        verify(salesBookRepository).search(any(), any(), any(), pageable.capture());
        var first = pageable.getValue().getSort().iterator().next();
        assertThat(first.getProperty()).isEqualTo("businessDate");
        assertThat(first.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getSalesBook_searchWithSpaces_isStrippedBeforeTheQuery() {
        // Arrange
        stubEmptyPage();

        // Act
        sut.getSalesBook(new PageQuery(1, 20, "  FAC-2026  ", null, null), FROM, FROM);

        // Assert
        verify(salesBookRepository).search(eq(FROM), eq(FROM), eq("FAC-2026"), any());
    }

    private static PageQuery query(String sortBy, SortDirection direction) {
        return new PageQuery(1, 20, null, sortBy, direction);
    }

    private void stubEmptyPage() {
        Page<SalesBookEntry> empty = new PageImpl<>(List.of());
        when(salesBookRepository.search(any(), any(), any(), any())).thenReturn(empty);
    }
}
