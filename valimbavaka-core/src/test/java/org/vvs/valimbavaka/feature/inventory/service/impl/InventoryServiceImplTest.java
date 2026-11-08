package org.vvs.valimbavaka.feature.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryArticle;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLine;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryLineTally;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryScope;
import org.vvs.valimbavaka.feature.inventory.domain.InventorySession;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionWDto;
import org.vvs.valimbavaka.feature.inventory.mapper.InventoryMapper;
import org.vvs.valimbavaka.feature.inventory.repository.InventoryLineRepository;
import org.vvs.valimbavaka.feature.inventory.repository.InventorySessionRepository;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.stock.service.StockService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    private static final StockLocationRDto STORE =
            new StockLocationRDto(InventoryTestData.LOCATION_ID, "STORE", "Boutique");
    private static final InventorySessionWDto REQUEST = new InventorySessionWDto(
            STORE.id(), InventoryScope.ALL, null, UUID.randomUUID(), LocalDate.of(2026, 11, 8));

    @Mock
    private InventorySessionRepository sessionRepository;

    @Mock
    private InventoryLineRepository lineRepository;

    @Mock
    private InventoryOpeningPolicy openingPolicy;

    @Mock
    private InventoryScopeResolver scopeResolver;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private StockService stockService;

    @Mock
    private ArticleService articleService;

    @Mock
    private JournalService journalService;

    private InventoryServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryServiceImpl(
                sessionRepository,
                lineRepository,
                openingPolicy,
                scopeResolver,
                new InventoryMapper(),
                referentialService,
                stockService,
                articleService,
                journalService);
    }

    @Test
    void openSession_validRequest_savesNumberedSessionWithOneLinePerArticleAndJournals() {
        // Arrange
        var opening = InventoryTestData.opening(null);
        when(openingPolicy.check(REQUEST)).thenReturn(opening);
        when(scopeResolver.articlesToCount(opening))
                .thenReturn(List.of(
                        new InventoryArticle(UUID.randomUUID(), "ART-0031", "Savon noir 100 g", "3210000000031"),
                        new InventoryArticle(UUID.randomUUID(), "ART-0087", "Huile de coco", null)));
        when(sessionRepository.nextNumber()).thenReturn(12L);
        when(sessionRepository.save(any(InventorySession.class))).thenAnswer(call -> call.getArgument(0));
        when(referentialService.getStockLocations(any(PageQuery.class)))
                .thenReturn(new PageResponse<>(List.of(STORE), 1, 100, 1, 1));

        // Act
        var result = sut.openSession(REQUEST);

        // Assert
        assertThat(result.number()).isEqualTo("INV-0012");
        assertThat(result.status()).isEqualTo(InventoryStatus.IN_PROGRESS);
        assertThat(result.locationCode()).isEqualTo("STORE");
        assertThat(result.responsibleName()).isEqualTo("Hanta Rakoto");
        assertThat(result.lineCount()).isEqualTo(2);
        assertThat(result.gapCount()).isZero();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InventoryLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(lineRepository).saveAll(lines.capture());
        assertThat(lines.getValue()).extracting(InventoryLine::getArticleCode).containsExactly("ART-0031", "ART-0087");
        assertThat(lines.getValue())
                .allSatisfy(line -> assertThat(line.getCountedQuantity()).isNull());
        verify(journalService)
                .record(eq(JournalAction.CREATE), eq("INVENTORY_SESSION"), any(), contains("INV-0012 · Boutique"));
    }

    @Test
    void openSession_emptyScope_throwsBusinessExceptionAndSavesNothing() {
        // Arrange
        var opening = InventoryTestData.opening(UUID.randomUUID());
        when(openingPolicy.check(REQUEST)).thenReturn(opening);
        when(scopeResolver.articlesToCount(opening)).thenReturn(List.of());

        // Act + Assert
        assertThatThrownBy(() -> sut.openSession(REQUEST))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aucun article");
        verify(sessionRepository, never()).save(any(InventorySession.class));
        verify(lineRepository, never()).saveAll(anyIterable());
        verifyNoInteractions(journalService);
    }

    @Test
    void getSession_unknownId_throwsNotFoundException() {
        // Arrange
        var id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getSession(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getSessions_closedSessions_returnsLineAndGapCountsReadInOneGroupedQuery() {
        // Arrange
        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        when(sessionRepository.search(eq(InventoryStatus.CLOSED), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sessionWithId(firstId), sessionWithId(secondId))));
        var first = new InventoryLineTally(firstId, 12L, 3L);
        var second = new InventoryLineTally(secondId, 4L, 0L);
        when(lineRepository.tallies(Set.of(firstId, secondId))).thenReturn(List.of(first, second));
        givenStore();

        // Act
        var result = sut.getSessions(new PageQuery(1, 20, null, null, null), InventoryStatus.CLOSED, null);

        // Assert
        assertThat(result.items()).extracting(InventorySessionRDto::id).containsExactly(firstId, secondId);
        assertThat(result.items()).extracting(InventorySessionRDto::lineCount).containsExactly(12L, 4L);
        assertThat(result.items()).extracting(InventorySessionRDto::gapCount).containsExactly(3L, 0L);
        verify(lineRepository).tallies(anyCollection());
    }

    @Test
    void getSessions_emptyPage_returnsNoItemAndDoesNotQueryTallies() {
        // Arrange
        when(sessionRepository.search(isNull(), isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        givenStore();

        // Act
        var result = sut.getSessions(new PageQuery(1, 20, null, null, null), null, null);

        // Assert
        assertThat(result.items()).isEmpty();
        verify(lineRepository, never()).tallies(anyCollection());
    }

    @Test
    void getSession_sessionWithGaps_returnsItsStatusAndGapCount() {
        // Arrange
        var id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.of(sessionWithId(id)));
        when(lineRepository.tallies(Set.of(id))).thenReturn(List.of(new InventoryLineTally(id, 2L, 1L)));
        givenStore();

        // Act
        var result = sut.getSession(id);

        // Assert
        assertThat(result.status()).isEqualTo(InventoryStatus.IN_PROGRESS);
        assertThat(result.lineCount()).isEqualTo(2);
        assertThat(result.gapCount()).isEqualTo(1);
    }

    @Test
    void getLines_lineNotCounted_returnsCurrentStockAsTheoreticalAndNoGap() {
        // Arrange
        var session = InventorySession.open("INV-0012", InventoryTestData.opening(null));
        var sessionId = UUID.randomUUID();
        var articleId = UUID.randomUUID();
        var line = InventoryLine.toCount(sessionId, new InventoryArticle(articleId, "ART-0031", "Savon noir", null));
        Pageable pageable = PageRequest.of(0, 20);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(lineRepository.findBySessionId(eq(sessionId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(line), pageable, 1));
        when(stockService.getQuantities(InventoryTestData.LOCATION_ID, Set.of(articleId)))
                .thenReturn(Map.of(articleId, new BigDecimal("42.000")));
        when(articleService.getCosts(Set.of(articleId))).thenReturn(Map.of(articleId, new BigDecimal("3120.50")));

        // Act
        var result = sut.getLines(sessionId, new PageQuery(1, 20, null, null, null));

        // Assert
        var item = result.items().getFirst();
        assertThat(item.theoreticalQuantity()).isEqualByComparingTo("42");
        assertThat(item.countedQuantity()).isNull();
        assertThat(item.gap()).isNull();
        assertThat(item.unitCost()).isEqualByComparingTo("3120.50");
    }

    private void givenStore() {
        when(referentialService.getStockLocations(any(PageQuery.class)))
                .thenReturn(new PageResponse<>(List.of(STORE), 1, 100, 1, 1));
    }

    private static InventorySession sessionWithId(UUID id) {
        var session = InventorySession.open("INV-0012", InventoryTestData.opening(null));
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
