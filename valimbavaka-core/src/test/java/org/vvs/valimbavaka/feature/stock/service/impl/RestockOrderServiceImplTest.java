package org.vvs.valimbavaka.feature.stock.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.stock.domain.RestockOrder;
import org.vvs.valimbavaka.feature.stock.domain.RestockOrderStatus;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderLineRDto;
import org.vvs.valimbavaka.feature.stock.dto.RestockOrderWDto;
import org.vvs.valimbavaka.feature.stock.dto.StockAlertRDto;
import org.vvs.valimbavaka.feature.stock.mapper.RestockOrderMapper;
import org.vvs.valimbavaka.feature.stock.repository.RestockOrderLineRepository;
import org.vvs.valimbavaka.feature.stock.repository.RestockOrderRepository;
import org.vvs.valimbavaka.feature.stock.service.StockService;

/** STK-05: a restock order is generated from the threshold alerts of the moment (STK-04). */
@ExtendWith(MockitoExtension.class)
class RestockOrderServiceImplTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID SUPPLIER_ID = UUID.randomUUID();
    private static final StockAlertRDto OUT = alert("ART-0002", "0", "50", null);
    private static final StockAlertRDto LOW = alert("ART-0001", "12.5", "40", SUPPLIER_ID);

    @Mock
    private StockService stockService;

    @Mock
    private RestockOrderRepository restockOrderRepository;

    @Mock
    private RestockOrderLineRepository restockOrderLineRepository;

    @Mock
    private JournalService journalService;

    private RestockOrderServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new RestockOrderServiceImpl(
                stockService,
                restockOrderRepository,
                restockOrderLineRepository,
                new RestockOrderMapper(),
                journalService);
    }

    @Test
    void generate_noArticleSelected_ordersEveryAlertMostUrgentFirstAndTracesIt() {
        // Arrange
        givenAlerts(OUT, LOW);
        givenSavedOrderNumber(7);

        // Act
        var result = sut.generate(new RestockOrderWDto(null));

        // Assert
        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.reference()).isEqualTo("BR-000007");
        assertThat(result.status()).isEqualTo(RestockOrderStatus.PENDING);
        assertThat(result.lines()).extracting(RestockOrderLineRDto::lineNumber).containsExactly(1, 2);
        assertThat(result.lines())
                .extracting(RestockOrderLineRDto::articleCode)
                .containsExactly("ART-0002", "ART-0001");
        var low = result.lines().get(1);
        assertThat(low.unit()).isEqualTo(ArticleUnit.PIECE);
        assertThat(low.stockQuantity()).isEqualByComparingTo("12.5");
        assertThat(low.threshold()).isEqualByComparingTo("40");
        assertThat(low.quantity()).isEqualByComparingTo("30");
        assertThat(low.supplierId()).isEqualTo(SUPPLIER_ID);
        assertThat(low.leadTimeDays()).isEqualTo(7);
        assertThat(low.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(result.lines().get(0).supplierId()).isNull();
        var description = "Génération du bon de réapprovisionnement BR-000007 : 2 article(s)";
        verify(journalService).record(JournalAction.CREATE, "RESTOCK_ORDER", ORDER_ID, description);
    }

    @Test
    void generate_selectedArticles_keepsOnlyTheSelectedOnesStillUnderThreshold() {
        // Arrange
        givenAlerts(OUT, LOW);
        givenSavedOrderNumber(8);
        var request = new RestockOrderWDto(List.of(LOW.articleId(), UUID.randomUUID(), LOW.articleId()));

        // Act
        var result = sut.generate(request);

        // Assert
        assertThat(result.lines()).extracting(RestockOrderLineRDto::articleId).containsExactly(LOW.articleId());
        assertThat(result.lines().get(0).lineNumber()).isEqualTo(1);
    }

    @Test
    void generate_emptySelection_ordersEveryAlert() {
        // Arrange
        givenAlerts(OUT, LOW);
        givenSavedOrderNumber(9);

        // Act
        var result = sut.generate(new RestockOrderWDto(List.of()));

        // Assert
        assertThat(result.lines()).hasSize(2);
    }

    @Test
    void generate_noSelectedArticleUnderThreshold_throwsConflictAndSavesNothing() {
        // Arrange
        givenAlerts(OUT);
        var request = new RestockOrderWDto(List.of(LOW.articleId()));

        // Act + Assert
        assertThatThrownBy(() -> sut.generate(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage(RestockOrderServiceImpl.NOTHING_TO_ORDER);
        verify(restockOrderRepository, never()).save(any());
        verify(journalService, never()).record(any(), any(), any(), any());
    }

    @Test
    void generate_noAlertAtAll_throwsConflict() {
        // Arrange
        givenAlerts();

        // Act + Assert
        assertThatThrownBy(() -> sut.generate(new RestockOrderWDto(null))).isInstanceOf(ConflictException.class);
        verify(restockOrderRepository, never()).nextNumber();
    }

    @Test
    void getRestockOrderById_unknownId_throwsNotFound() {
        // Arrange
        var id = UUID.randomUUID();
        when(restockOrderRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getRestockOrderById(id)).isInstanceOf(NotFoundException.class);
        verify(restockOrderLineRepository, never()).findLinesOf(any());
    }

    @Test
    void getRestockOrderById_knownId_returnsItsLines() {
        // Arrange
        var order = RestockOrder.create(3);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        when(restockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(restockOrderLineRepository.findLinesOf(List.of(ORDER_ID))).thenReturn(List.of());

        // Act
        var result = sut.getRestockOrderById(ORDER_ID);

        // Assert
        assertThat(result.reference()).isEqualTo("BR-000003");
        assertThat(result.lines()).isEmpty();
    }

    private void givenAlerts(StockAlertRDto... alerts) {
        when(stockService.getAllAlerts()).thenReturn(List.of(alerts));
    }

    private void givenSavedOrderNumber(long number) {
        when(restockOrderRepository.nextNumber()).thenReturn(number);
        when(restockOrderRepository.save(any(RestockOrder.class))).thenAnswer(invocation -> {
            RestockOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", ORDER_ID);
            return order;
        });
        when(restockOrderLineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static StockAlertRDto alert(String code, String quantity, String threshold, UUID supplierId) {
        var stock = new BigDecimal(quantity);
        var limit = new BigDecimal(threshold);
        var shortage = limit.subtract(stock);
        var withSupplier = supplierId != null;
        return new StockAlertRDto(
                UUID.randomUUID(),
                code,
                "Article " + code,
                ArticleUnit.PIECE,
                stock,
                limit,
                shortage,
                StockAlertPolicy.suggestedQuantity(shortage),
                supplierId,
                withSupplier ? "Huiles Ravo" : null,
                withSupplier ? 7 : null,
                withSupplier ? LocalDate.of(2026, 11, 15) : null);
    }
}
